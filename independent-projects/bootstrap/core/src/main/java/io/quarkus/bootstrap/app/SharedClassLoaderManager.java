package io.quarkus.bootstrap.app;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Manages shared augmentation and base runtime class loaders across multiple {@link CuratedApplication} instances.
 * <p>
 * When running tests, each test class or test profile creates its own {@code CuratedApplication} and thus its own
 * augmentation and base runtime class loaders. Without sharing, each instance loads the same deployment and runtime
 * classes into its own class loaders, which costs a lot of time (class loading, JIT warm-up, GC) and Metaspace memory.
 * <p>
 * This manager holds shared class loaders that are built by the first {@code CuratedApplication} requesting a given
 * configuration. Subsequent instances with compatible configurations reuse the same class loaders, so classes are
 * loaded only once per distinct configuration.
 * <p>
 * Each shared class loader uses reference counting. When the last reference is released, the class loader is either
 * closed right away, or, when the manager retains idle class loaders, kept open so that the next
 * {@code CuratedApplication} with a compatible configuration can reuse it (tests typically run sequentially so there
 * is usually no overlap). Idle class loaders are closed when a {@code CuratedApplication} requires an incompatible
 * configuration, so that at most one configuration is kept alive at a time when idle, and when the manager itself is
 * {@linkplain #close() closed}. Retaining idle class loaders requires the manager to be closed at the end of the test
 * run, otherwise the close tasks registered on the class loaders (e.g. shutting down the dev services) never run.
 * <p>
 * Sharing the base runtime class loader is only safe when the application is started in a fresh runtime class loader
 * (i.e. not with a flat test class path). Inputs that cannot be shared are built and closed as with
 * {@link DefaultClassLoaderManager}.
 * <p>
 * Instances of this class should be created by the test framework and passed to
 * {@link QuarkusBootstrap.Builder#setClassLoaderManager(ClassLoaderManager)}.
 * The instance should be scoped to a single module's test execution (e.g., one Surefire/Failsafe run),
 * we don't want to share the class loaders between runs.
 */
public class SharedClassLoaderManager implements ClassLoaderManager, Closeable {

    private static final Logger log = Logger.getLogger(SharedClassLoaderManager.class);

    private final Pool<AugmentationClassLoaderInput, AugmentationClassLoaderResult> augmentationClassLoaders;
    private final Pool<BaseRuntimeClassLoaderInput, QuarkusClassLoader> baseRuntimeClassLoaders;

    /**
     * Creates a manager sharing the augmentation class loaders only, closing them when their last reference is
     * released.
     */
    public SharedClassLoaderManager() {
        this(false, false);
    }

    /**
     * @param shareBaseRuntimeClassLoaders whether the base runtime class loaders should be shared too
     * @param retainIdleClassLoaders whether the class loaders should be kept open when their last reference is
     *        released, until an incompatible configuration is requested or the manager is closed
     */
    public SharedClassLoaderManager(boolean shareBaseRuntimeClassLoaders, boolean retainIdleClassLoaders) {
        this.augmentationClassLoaders = new Pool<>("augmentation", AugmentationClassLoaderResult::classLoader,
                retainIdleClassLoaders);
        this.baseRuntimeClassLoaders = shareBaseRuntimeClassLoaders
                ? new Pool<>("base runtime", Function.identity(), retainIdleClassLoaders)
                : null;
    }

    @Override
    public AugmentationClassLoaderResult getOrCreateAugmentationClassLoader(AugmentationClassLoaderInput input,
            Function<AugmentationClassLoaderInput, AugmentationClassLoaderResult> factory) {
        return augmentationClassLoaders.getOrCreate(input, factory);
    }

    @Override
    public void releaseAugmentationClassLoader(QuarkusClassLoader augmentClassLoader) {
        augmentationClassLoaders.release(augmentClassLoader);
    }

    @Override
    public QuarkusClassLoader getOrCreateBaseRuntimeClassLoader(BaseRuntimeClassLoaderInput input,
            Function<BaseRuntimeClassLoaderInput, QuarkusClassLoader> factory) {
        if (baseRuntimeClassLoaders == null) {
            return factory.apply(input);
        }
        return baseRuntimeClassLoaders.getOrCreate(input, factory);
    }

    @Override
    public void releaseBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader) {
        if (baseRuntimeClassLoaders == null) {
            baseRuntimeClassLoader.close();
            return;
        }
        baseRuntimeClassLoaders.release(baseRuntimeClassLoader);
    }

    @Override
    public boolean isSharedBaseRuntimeClassLoader(QuarkusClassLoader baseRuntimeClassLoader) {
        return baseRuntimeClassLoaders != null && baseRuntimeClassLoaders.contains(baseRuntimeClassLoader);
    }

    /**
     * Closes all the idle shared class loaders. Class loaders still in use are closed when they get released.
     */
    @Override
    public void close() {
        augmentationClassLoaders.close();
        if (baseRuntimeClassLoaders != null) {
            baseRuntimeClassLoaders.close();
        }
    }

    private static class Pool<I extends SharedClassLoaderInput<I>, R> {

        private final String kind;
        private final Function<R, QuarkusClassLoader> classLoaderExtractor;
        private final boolean retainIdle;
        private final List<Entry<I, R>> entries = new ArrayList<>();
        private boolean closed;

        Pool(String kind, Function<R, QuarkusClassLoader> classLoaderExtractor, boolean retainIdle) {
            this.kind = kind;
            this.classLoaderExtractor = classLoaderExtractor;
            this.retainIdle = retainIdle;
        }

        synchronized R getOrCreate(I input, Function<I, R> factory) {
            if (closed || !input.isShareable()) {
                // not tracked: release() will simply close it
                return factory.apply(input);
            }
            for (Entry<I, R> entry : entries) {
                if (entry.input.isCompatibleWith(input)) {
                    entry.refCount++;
                    log.debugf("Reusing shared %s class loader %s (refCount=%d)", kind,
                            classLoaderExtractor.apply(entry.result), entry.refCount);
                    return entry.result;
                }
            }
            // the configuration is not compatible with the idle class loaders, close them so that we don't keep
            // too many class loaders alive
            closeIdleEntries();
            R result = factory.apply(input.withClassLoaderNameSuffix(" (shared)"));
            entries.add(new Entry<>(input, result));
            log.debugf("Created shared %s class loader %s (total=%d)", kind, classLoaderExtractor.apply(result),
                    entries.size());
            return result;
        }

        synchronized void release(QuarkusClassLoader classLoader) {
            Iterator<Entry<I, R>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<I, R> entry = iterator.next();
                if (classLoaderExtractor.apply(entry.result) == classLoader) {
                    entry.refCount--;
                    if (entry.refCount == 0 && (closed || !retainIdle)) {
                        log.debugf("Closing shared %s class loader %s (last reference released)", kind, classLoader);
                        iterator.remove();
                        classLoader.close();
                    } else {
                        log.debugf("Released shared %s class loader %s (refCount=%d)", kind, classLoader,
                                entry.refCount);
                    }
                    return;
                }
            }
            // not a shared class loader
            classLoader.close();
        }

        synchronized boolean contains(QuarkusClassLoader classLoader) {
            for (Entry<I, R> entry : entries) {
                if (classLoaderExtractor.apply(entry.result) == classLoader) {
                    return true;
                }
            }
            return false;
        }

        synchronized void close() {
            closed = true;
            closeIdleEntries();
        }

        private void closeIdleEntries() {
            Iterator<Entry<I, R>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Entry<I, R> entry = iterator.next();
                if (entry.refCount == 0) {
                    QuarkusClassLoader classLoader = classLoaderExtractor.apply(entry.result);
                    log.debugf("Closing idle shared %s class loader %s", kind, classLoader);
                    iterator.remove();
                    classLoader.close();
                }
            }
        }
    }

    private static class Entry<I, R> {

        final I input;
        final R result;
        int refCount = 1;

        Entry(I input, R result) {
            this.input = input;
            this.result = result;
        }
    }
}
