package io.quarkus.smallrye.health.runtime;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarNotFoundHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.smallrye.health.SmallRyeHealthReporter;
import io.smallrye.health.registry.HealthRegistries;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class SmallRyeHealthRecorder {

    private static final Logger LOG = Logger.getLogger(SmallRyeHealthRecorder.class);

    // default value used by SmallRye Health
    private static final int DEFAULT_MAX_GROUP_REGISTRIES_COUNT = 100;
    private final RuntimeValue<SmallRyeHealthRuntimeConfig> runtimeConfig;

    public SmallRyeHealthRecorder(final RuntimeValue<SmallRyeHealthRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public static void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

    public Handler<RoutingContext> uiHandler(String healthUiFinalDestination, String healthUiPath,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations, ShutdownContext shutdownContext) {

        if (runtimeConfig.getValue().enabled()) {
            WebJarStaticHandler handler = new WebJarStaticHandler(healthUiFinalDestination, healthUiPath,
                    webRootConfigurations);
            shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
            return handler;
        } else {
            return new WebJarNotFoundHandler();
        }
    }

    public static void processSmallRyeHealthRuntimeConfiguration(BeanContainer container,
            SmallRyeHealthRuntimeConfig runtimeConfig, SmallRyeHealthBuildFixedConfig buildFixedConfig) {
        SmallRyeHealthReporter reporter = container.beanInstance(SmallRyeHealthReporter.class);
        reporter.setAdditionalProperties(runtimeConfig.additionalProperties());

        reporter.setHealthChecksConfigs(runtimeConfig.check().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().enabled())));

        SmallRyeHealthHandlerBase.problemDetails = buildFixedConfig.includeProblemDetails();
        syncMaxGroupRegistriesCount();
    }

    /**
     * SmallRye Health reads {@code io.smallrye.health.maxGroupRegistriesCount} once, when the static registries
     * class is initialized. When the classes are shared by several applications (dev mode restarts, tests sharing
     * the runtime class loader), the value configured by the current application must be applied explicitly.
     */
    private static void syncMaxGroupRegistriesCount() {
        try {
            int maxGroupRegistriesCount = ConfigProvider.getConfig()
                    .getOptionalValue("io.smallrye.health.maxGroupRegistriesCount", Integer.class)
                    .orElse(DEFAULT_MAX_GROUP_REGISTRIES_COUNT);
            Field field = HealthRegistries.class.getDeclaredField("maxGroupRegistriesCount");
            field.setAccessible(true);
            field.setInt(null, maxGroupRegistriesCount);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("Unable to set the maximum number of SmallRye Health group registries", e);
        }
    }

    /**
     * The SmallRye Health registries are static: the health checks and groups registered by the application must be
     * dropped when it shuts down, otherwise they would still be there for the next application using the same
     * classes (dev mode restarts, tests sharing the runtime class loader).
     * There is no API to reset them so we have to resort to reflection.
     */
    public static void resetHealthRegistries() {
        for (String fieldName : List.of("registries", "groupRegistries")) {
            try {
                Field field = HealthRegistries.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                ((Map<?, ?>) field.get(null)).clear();
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOG.warn("Unable to reset the SmallRye Health registries", e);
            }
        }
    }

}
