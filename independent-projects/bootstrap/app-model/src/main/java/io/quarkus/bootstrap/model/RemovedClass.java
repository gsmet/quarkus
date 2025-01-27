package io.quarkus.bootstrap.model;

import java.util.Objects;
import java.util.function.Predicate;

public class RemovedClass {

    private static final String CLASS_SUFFIX = ".class";

    private final Predicate<String> classNamePredicate;

    public RemovedClass(Predicate<String> classNamePredicate) {
        this.classNamePredicate = classNamePredicate;
    }

    public boolean matchesClassName(String className) {
        Objects.requireNonNull(className);

        return classNamePredicate.test(className);
    }

    public boolean matchesFileName(String fileName) {
        Objects.requireNonNull(fileName);

        if (!fileName.endsWith(CLASS_SUFFIX)) {
            throw new IllegalArgumentException(
                    fileName + " is not a valid class name as it doesn't end with " + CLASS_SUFFIX);
        }

        return classNamePredicate.test(fileName.replace('/', '.').substring(0, fileName.length() - CLASS_SUFFIX.length()));
    }
}