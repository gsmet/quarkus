package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.invoke.Invoker;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.AbstractAnnotationLiteral;
import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.Components;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.InterceptionProxySubclass;
import io.quarkus.arc.Lock;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.ContextInstances;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.gizmo2.GenericType;

final class ArcGenericTypes {

    static final GenericType.OfClass OBJECT = GenericType.ofClass(Object.class);
    static final GenericType.OfArray OBJECT_ARRAY = GenericType.ofArray(Object[].class);
    static final GenericType.OfClass CLASS = GenericType.ofClass(Class.class);
    static final GenericType.OfClass STRING = GenericType.ofClass(String.class);
    static final GenericType.OfClass SET = GenericType.ofClass(Set.class);
    static final GenericType.OfClass LIST = GenericType.ofClass(List.class);
    static final GenericType.OfClass ARRAY_LIST = GenericType.ofClass(ArrayList.class);
    static final GenericType.OfClass MAP = GenericType.ofClass(Map.class);
    static final GenericType.OfClass SUPPLIER = GenericType.ofClass(Supplier.class);
    static final GenericType.OfClass CONSUMER = GenericType.ofClass(Consumer.class);
    static final GenericType.OfClass RUNNABLE = GenericType.ofClass(Runnable.class);
    static final GenericType.OfClass ATOMIC_REFERENCE = GenericType.ofClass(AtomicReference.class);
    static final GenericType.OfClass ATOMIC_REFERENCE_FIELD_UPDATER = GenericType.ofClass(AtomicReferenceFieldUpdater.class);
    static final GenericType.OfClass REFLECT_TYPE = GenericType.ofClass(java.lang.reflect.Type.class);
    static final GenericType.OfClass INTERCEPTED_METHOD_METADATA = GenericType.ofClass(InterceptedMethodMetadata.class);
    static final GenericType.OfClass INJECTABLE_BEAN = GenericType.ofClass(InjectableBean.class);
    static final GenericType.OfClass INJECTABLE_BEAN_KIND = GenericType.ofClass(InjectableBean.Kind.class);
    static final GenericType.OfClass INJECTABLE_CONTEXT = GenericType.ofClass(InjectableContext.class);
    static final GenericType.OfClass INJECTABLE_CONTEXT_CONTEXT_STATE = GenericType
            .ofClass(InjectableContext.ContextState.class);
    static final GenericType.OfClass CONTEXT_INSTANCE_HANDLE = GenericType.ofClass(ContextInstanceHandle.class);
    static final GenericType.OfClass LOCK = GenericType.ofClass(Lock.class);
    static final GenericType.OfClass CLIENT_PROXY = GenericType.ofClass(ClientProxy.class);
    static final GenericType.OfClass MOCKABLE = GenericType.ofClass(Mockable.class);
    static final GenericType.OfClass COMPONENTS_PROVIDER = GenericType.ofClass(ComponentsProvider.class);
    static final GenericType.OfClass CONTEXT_INSTANCES = GenericType.ofClass(ContextInstances.class);
    static final GenericType.OfClass INJECTABLE_DECORATOR = GenericType.ofClass(InjectableDecorator.class);
    static final GenericType.OfClass INJECTABLE_REFERENCE_PROVIDER = GenericType.ofClass(InjectableReferenceProvider.class);
    static final GenericType.OfClass INTERCEPTION_PROXY = GenericType.ofClass(InterceptionProxy.class);
    static final GenericType.OfClass INTERCEPTION_PROXY_SUBCLASS = GenericType.ofClass(InterceptionProxySubclass.class);
    static final GenericType.OfClass INJECTABLE_INTERCEPTOR = GenericType.ofClass(InjectableInterceptor.class);
    static final GenericType.OfClass INJECTABLE_OBSERVER_METHOD = GenericType.ofClass(InjectableObserverMethod.class);
    static final GenericType.OfClass ACTIVE_RESULT = GenericType.ofClass(ActiveResult.class);
    static final GenericType.OfClass COMPONENTS = GenericType.ofClass(Components.class);
    static final GenericType.OfClass SUBCLASS = GenericType.ofClass(Subclass.class);
    static final GenericType.OfClass ABSTRACT_ANNOTATION_LITERAL = GenericType.ofClass(AbstractAnnotationLiteral.class);
    static final GenericType.OfClass CREATIONAL_CONTEXT = GenericType.ofClass(CreationalContext.class);
    static final GenericType.OfClass SYNTHETIC_CREATIONAL_CONTEXT = GenericType.ofClass(SyntheticCreationalContext.class);
    static final GenericType.OfClass CURRENT_CONTEXT_FACTORY = GenericType.ofClass(CurrentContextFactory.class);
    static final GenericType.OfClass INVOKER = GenericType.ofClass(Invoker.class);
    static final GenericType.OfClass INTERCEPTION_TYPE = GenericType.ofClass(InterceptionType.class);
    static final GenericType.OfClass INVOCATION_CONTEXT = GenericType.ofClass(InvocationContext.class);
    static final GenericType.OfClass EVENT_CONTEXT = GenericType.ofClass(EventContext.class);

    private ArcGenericTypes() {
    }
}
