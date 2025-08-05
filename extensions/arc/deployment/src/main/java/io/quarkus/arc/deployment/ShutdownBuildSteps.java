package io.quarkus.arc.deployment;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ObserverRegistrationPhaseBuildItem.ObserverConfiguratorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.ShutdownEvent;

public class ShutdownBuildSteps {

    static final DotName SHUTDOWN_NAME = DotName.createSimple(Shutdown.class.getName());

    private static final Logger LOG = Logger.getLogger(ShutdownBuildSteps.class);

    @BuildStep
    AutoAddScopeBuildItem addScope(CustomScopeAnnotationsBuildItem customScopes) {
        // Class with no built-in scope annotation but with @Shutdown annotation
        return AutoAddScopeBuildItem.builder()
                .defaultScope(BuiltinScope.APPLICATION)
                .anyMethodMatches(new Predicate<MethodInfo>() {
                    @Override
                    public boolean test(MethodInfo m) {
                        return m.hasAnnotation(SHUTDOWN_NAME);
                    }
                })
                .reason("Found classes containing @Shutdown annotation.")
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo bean) {
                if (bean.isClassBean()) {
                    return bean.getTarget().get().asClass().annotationsMap().containsKey(SHUTDOWN_NAME);
                }
                return false;
            }
        });
    }

    @BuildStep
    void registerShutdownObservers(ObserverRegistrationPhaseBuildItem observerRegistration,
            BuildProducer<ObserverConfiguratorBuildItem> configurators) {

        AnnotationStore annotationStore = observerRegistration.getContext().get(BuildExtension.Key.ANNOTATION_STORE);

        for (BeanInfo bean : observerRegistration.getContext().beans().classBeans()) {
            ClassInfo beanClass = bean.getTarget().get().asClass();
            List<MethodInfo> shutdownMethods = new ArrayList<>();
            // Collect all non-static no-args methods annotated with @Shutdown
            for (MethodInfo method : beanClass.methods()) {
                if (annotationStore.hasAnnotation(method, SHUTDOWN_NAME)) {
                    if (!method.isSynthetic()
                            && !Modifier.isPrivate(method.flags())
                            && !Modifier.isStatic(method.flags())
                            && method.parametersCount() == 0) {
                        shutdownMethods.add(method);
                    } else {
                        LOG.warnf("Ignored an invalid @Shutdown method declared on %s: %s", method.declaringClass().name(),
                                method);
                    }
                }
            }
            if (!shutdownMethods.isEmpty()) {
                for (MethodInfo method : shutdownMethods) {
                    AnnotationValue priority = annotationStore.getAnnotation(method, SHUTDOWN_NAME).value();
                    registerShutdownObserver(observerRegistration, bean,
                            method.declaringClass().name() + "#" + method.toString(),
                            priority != null ? priority.asInt() : ObserverMethod.DEFAULT_PRIORITY, method);
                }
            }
        }
    }

    private void registerShutdownObserver(ObserverRegistrationPhaseBuildItem observerRegistration, BeanInfo btBean, String id,
            int priority, MethodInfo shutdownMethod) {
        ObserverConfigurator configurator = observerRegistration.getContext().configure()
                .beanClass(btBean.getBeanClass())
                .observedType(ShutdownEvent.class);
        configurator.id(id);
        configurator.priority(priority);
        configurator.notify(ng -> {
            BlockCreator b0 = ng.notifyMethod();

            // InjectableBean<Foo> bean = Arc.container().bean("bflmpsvz");
            LocalVar arc = b0.localVar("arc", b0.invokeStatic(StartupBuildSteps.ARC_CONTAINER));
            LocalVar rtBean = b0.localVar("bean",
                    b0.invokeInterface(StartupBuildSteps.ARC_CONTAINER_BEAN, arc, Const.of(btBean.getIdentifier())));
            if (BuiltinScope.DEPENDENT.is(btBean.getScope())) {
                LocalVar creationalContext = b0.localVar("creationalContext",
                        b0.new_(StartupBuildSteps.CREATIONAL_CONTEXT_IMPL_CTOR, rtBean));
                // Create a dependent instance
                LocalVar instance = b0.localVar("instance",
                        b0.invokeInterface(StartupBuildSteps.CONTEXTUAL_CREATE, rtBean, creationalContext));
                b0.try_(tc -> {
                    tc.body(b1 -> {
                        b1.invokeVirtual(methodDescOf(shutdownMethod), instance);
                    });
                    tc.catch_(Exception.class, "e", (b1, e) -> {
                        b1.throw_(b1.new_(ConstructorDesc.of(RuntimeException.class, String.class, Throwable.class),
                                Const.of("Error calling @Shutdown method"), e));
                    });
                    tc.finally_(b1 -> {
                        // Destroy the instance immediately
                        b1.invokeInterface(StartupBuildSteps.CONTEXTUAL_DESTROY, rtBean, instance, creationalContext);
                    });
                });
            } else {
                // Obtains the instance from the context
                // InstanceHandle<Foo> handle = Arc.container().instance(bean);
                Expr instanceHandle = b0.invokeInterface(StartupBuildSteps.ARC_CONTAINER_INSTANCE, arc, rtBean);
                Expr instance = b0.invokeInterface(StartupBuildSteps.INSTANCE_HANDLE_GET, instanceHandle);
                b0.invokeVirtual(methodDescOf(shutdownMethod), instance);
            }
            b0.return_();
        });
        configurator.done();
    }
}
