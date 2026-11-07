package io.quarkus.bootstrap.app;

import java.util.function.Function;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Strategy for managing the augmentation and base runtime class loaders across {@link CuratedApplication} instances.
 * <p>
 * Implementations control whether these class loaders are shared (to save Metaspace and avoid loading the same classes
 * again and again) or standalone (one per {@code CuratedApplication}).
 *
 * @see DefaultClassLoaderManager
 * @see SharedClassLoaderManager
 */
public interface ClassLoaderManager {

    /**
     * Get or create the augmentation class loader.
     * <p>
     * The supplied factory builds the class loader from the given input.
     * Implementations may call it immediately or return a previously built shared instance.
     *
     * @return the class loader and its element cache, never {@code null}
     */
    AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory);

    /**
     * Release the augmentation class loader.
     * <p>
     * For standalone managers, this closes the class loader immediately.
     * For shared managers, this decrements the reference count; the class loader may be kept open for reuse.
     * If the class loader is not managed (e.g. due to incompatible configs), it is closed directly.
     *
     * @param augmentClassLoader the class loader to release
     */
    void releaseAugmentationClassLoader(QuarkusClassLoader augmentClassLoader);

    /**
     * Get or create the base runtime class loader.
     * <p>
     * The supplied factory builds the class loader from the given input.
     * Implementations may call it immediately or return a previously built shared instance.
     *
     * @return the class loader, never {@code null}
     */
    QuarkusClassLoader getOrCreateBaseRuntimeClassLoader(BaseRuntimeClassLoaderInput input,
            Function<BaseRuntimeClassLoaderInput, QuarkusClassLoader> factory);

    /**
     * Release the base runtime class loader.
     * <p>
     * Same semantics as {@link #releaseAugmentationClassLoader(QuarkusClassLoader)}.
     *
     * @param baseRuntimeClassLoader the class loader to release
     */
    void releaseBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader);

    /**
     * @return whether the given base runtime class loader is shared with other {@link CuratedApplication}s, in which
     *         case a {@code CuratedApplication} may not use it if it turns out to be incompatible with its application
     *         (see {@link CuratedApplication#resetBaseRuntimeClassLoader(java.util.Map, java.util.Map)})
     */
    boolean isSharedBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader);
}
