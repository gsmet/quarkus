package io.quarkus.bootstrap.app;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.bootstrap.classloading.ClassLoaderEventListener;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;

/**
 * All inputs that determine the contents of a base runtime class loader.
 * <p>
 * This record serves two purposes:
 * <ol>
 * <li>It is the explicit parameter list for the static
 * {@link CuratedApplication#buildBaseRuntimeClassLoader} factory method,
 * making all dependencies visible.</li>
 * <li>Its {@link #isCompatibleWith} method is the single source of truth for
 * whether two base runtime class loaders can be shared.</li>
 * </ol>
 * Note that the content of the application root is not part of the input: it is banned from the base runtime
 * class loader (or, with a flat test class path, loaded by it, in which case the class loader cannot be shared)
 * and the banned resources are refreshed each time the class loader is reset for a new start.
 */
public record BaseRuntimeClassLoaderInput(
        // Content-affecting fields, compared in isCompatibleWith()
        // (the dependencies are compared with ClassLoaderInputs#sameClassPath)
        ConfiguredClassLoading configuredClassLoading,
        PathCollection applicationRoot,
        List<AdditionalDependency> additionalApplicationArchives,
        List<ResolvedDependency> dependencies,
        Set<ArtifactKey> reloadableWorkspaceDependencies,
        ClassLoader baseClassLoader,
        boolean assertionsEnabled,
        List<ClassLoaderEventListener> classLoaderEventListeners,

        // Structural fields
        // The class path elements already created for the augmentation class loader, so that they can be reused.
        // The elements are equivalent for compatible inputs. Not compared in isCompatibleWith().
        Map<ArtifactKey, ClassPathElement> elementCache,

        // Cosmetic fields
        // Affect the CL display name only, not its content.
        QuarkusBootstrap.Mode mode,
        String classLoaderNameSuffix) implements SharedClassLoaderInput<BaseRuntimeClassLoaderInput> {

    public BaseRuntimeClassLoaderInput(ConfiguredClassLoading configuredClassLoading,
            PathCollection applicationRoot,
            List<AdditionalDependency> additionalApplicationArchives,
            Collection<ResolvedDependency> dependencies,
            Set<ArtifactKey> reloadableWorkspaceDependencies,
            ClassLoader baseClassLoader,
            boolean assertionsEnabled,
            List<ClassLoaderEventListener> classLoaderEventListeners,
            Map<ArtifactKey, ClassPathElement> elementCache,
            QuarkusBootstrap.Mode mode,
            String classLoaderNameSuffix) {
        this(configuredClassLoading, applicationRoot, List.copyOf(additionalApplicationArchives),
                List.copyOf(dependencies), Set.copyOf(reloadableWorkspaceDependencies), baseClassLoader,
                assertionsEnabled, List.copyOf(classLoaderEventListeners), elementCache, mode, classLoaderNameSuffix);
    }

    /**
     * With a flat test class path, the base runtime class loader is the runtime class loader of the application:
     * it loads the application classes and the generated application classes, so it cannot be shared.
     */
    @Override
    public boolean isShareable() {
        return !configuredClassLoading.isFlatTestClassPath();
    }

    @Override
    public boolean isCompatibleWith(BaseRuntimeClassLoaderInput other) {
        return isShareable() && other.isShareable()
                && configuredClassLoading.isCompatibleForBaseRuntimeWith(other.configuredClassLoading)
                && applicationRoot.equals(other.applicationRoot)
                && additionalApplicationArchives.equals(other.additionalApplicationArchives)
                && ClassLoaderInputs.sameClassPath(dependencies, other.dependencies)
                && reloadableWorkspaceDependencies.equals(other.reloadableWorkspaceDependencies)
                && baseClassLoader == other.baseClassLoader
                && assertionsEnabled == other.assertionsEnabled
                && classLoaderEventListeners.equals(other.classLoaderEventListeners);
    }

    @Override
    public BaseRuntimeClassLoaderInput withClassLoaderNameSuffix(String classLoaderNameSuffix) {
        return new BaseRuntimeClassLoaderInput(configuredClassLoading, applicationRoot, additionalApplicationArchives,
                dependencies, reloadableWorkspaceDependencies, baseClassLoader, assertionsEnabled,
                classLoaderEventListeners, elementCache, mode, classLoaderNameSuffix);
    }
}
