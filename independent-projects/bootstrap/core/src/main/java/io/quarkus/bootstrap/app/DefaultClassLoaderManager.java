package io.quarkus.bootstrap.app;

import java.util.function.Function;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Default (non-sharing) class loader manager.
 * <p>
 * Each {@link CuratedApplication} using this manager gets its own augmentation and base runtime class loaders.
 * The class loaders are built by calling the factory and closed when released.
 * <p>
 * This implementation is stateless and can safely be shared across multiple
 * {@code CuratedApplication} instances without risk of interleaving.
 */
public class DefaultClassLoaderManager implements ClassLoaderManager {

    private static final DefaultClassLoaderManager INSTANCE = new DefaultClassLoaderManager();

    public static DefaultClassLoaderManager getInstance() {
        return INSTANCE;
    }

    @Override
    public AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory) {
        return factory.apply(input);
    }

    @Override
    public void releaseAugmentationClassLoader(QuarkusClassLoader augmentClassLoader) {
        augmentClassLoader.close();
    }

    @Override
    public QuarkusClassLoader getOrCreateBaseRuntimeClassLoader(BaseRuntimeClassLoaderInput input,
            Function<BaseRuntimeClassLoaderInput, QuarkusClassLoader> factory) {
        return factory.apply(input);
    }

    @Override
    public void releaseBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader) {
        baseRuntimeClassLoader.close();
    }

    @Override
    public boolean isSharedBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader) {
        return false;
    }
}
