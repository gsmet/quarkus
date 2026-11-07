package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * All inputs that determine the contents of an augmentation class loader.
 * <p>
 * This record serves two purposes:
 * <ol>
 * <li>It is the explicit parameter list for the static
 * {@link CuratedApplication#buildAugmentationClassLoader} factory method,
 * making all dependencies visible.</li>
 * <li>Its {@link #isCompatibleWith} method is the single source of truth for
 * whether two augmentation class loaders can be shared.</li>
 * </ol>
 */
public record AugmentationClassLoaderInput(
        // Content-affecting fields, compared in isCompatibleWith()
        // (the dependencies are compared with ClassLoaderInputs#sameClassPath)
        ConfiguredClassLoading configuredClassLoading,
        List<Path> additionalDeploymentArchives,
        List<ResolvedDependency> dependencies,
        ClassLoader baseClassLoader,
        boolean isolateDeployment,
        boolean assertionsEnabled,
        List<ClassLoaderEventListener> classLoaderEventListeners,

        // Cosmetic fields
        // Affect the CL display name only, not its content.
        QuarkusBootstrap.Mode mode,
        String classLoaderNameSuffix) implements SharedClassLoaderInput<AugmentationClassLoaderInput> {

    public AugmentationClassLoaderInput(ConfiguredClassLoading configuredClassLoading,
            List<Path> additionalDeploymentArchives,
            Collection<ResolvedDependency> dependencies,
            ClassLoader baseClassLoader,
            boolean isolateDeployment,
            boolean assertionsEnabled,
            List<ClassLoaderEventListener> classLoaderEventListeners,
            QuarkusBootstrap.Mode mode,
            String classLoaderNameSuffix) {
        this(configuredClassLoading, List.copyOf(additionalDeploymentArchives), List.copyOf(dependencies),
                baseClassLoader, isolateDeployment, assertionsEnabled, List.copyOf(classLoaderEventListeners), mode,
                classLoaderNameSuffix);
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    /**
     * Check whether this input is compatible with another for the purpose of
     * sharing an augmentation class loader.
     * <p>
     * Delegates to {@link ConfiguredClassLoading#isCompatibleForAugmentationWith}
     * for classloading configuration fields, and additionally compares all the other
     * fields affecting the content of the class loader.
     */
    @Override
    public boolean isCompatibleWith(AugmentationClassLoaderInput other) {
        return configuredClassLoading.isCompatibleForAugmentationWith(other.configuredClassLoading)
                && additionalDeploymentArchives.equals(other.additionalDeploymentArchives)
                && ClassLoaderInputs.sameClassPath(dependencies, other.dependencies)
                && baseClassLoader == other.baseClassLoader
                && isolateDeployment == other.isolateDeployment
                && assertionsEnabled == other.assertionsEnabled
                && classLoaderEventListeners.equals(other.classLoaderEventListeners);
    }

    @Override
    public AugmentationClassLoaderInput withClassLoaderNameSuffix(String classLoaderNameSuffix) {
        return new AugmentationClassLoaderInput(configuredClassLoading, additionalDeploymentArchives, dependencies,
                baseClassLoader, isolateDeployment, assertionsEnabled, classLoaderEventListeners, mode,
                classLoaderNameSuffix);
    }
}
