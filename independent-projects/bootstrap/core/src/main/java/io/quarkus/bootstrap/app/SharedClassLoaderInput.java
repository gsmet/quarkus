package io.quarkus.bootstrap.app;

/**
 * The inputs determining the content of a class loader that may be shared by a {@link SharedClassLoaderManager}.
 *
 * @param <I> the input type
 */
public interface SharedClassLoaderInput<I extends SharedClassLoaderInput<I>> {

    /**
     * @return whether a class loader built from this input may be shared at all
     */
    boolean isShareable();

    /**
     * Check whether this input is compatible with another for the purpose of sharing a class loader,
     * i.e. whether both inputs would produce class loaders with the same content.
     */
    boolean isCompatibleWith(I other);

    /**
     * @return a copy of this input with the given class loader name suffix (which is cosmetic, it does not affect
     *         the content of the class loader)
     */
    I withClassLoaderNameSuffix(String classLoaderNameSuffix);
}
