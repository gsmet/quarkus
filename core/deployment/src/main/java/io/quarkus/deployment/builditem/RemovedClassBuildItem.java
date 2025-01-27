package io.quarkus.deployment.builditem;

import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Represents classes to be removed from a dependency when packaging the application.
 */
public final class RemovedClassBuildItem extends MultiBuildItem {

    private final ArtifactKey artifact;
    private final Predicate<String> classNamePredicate;

    public static RemovedClassBuildItem ofPredicate(ArtifactKey artifact, Predicate<String> classNamePredicate) {
        return new RemovedClassBuildItem(artifact, classNamePredicate);
    }

    public static RemovedClassBuildItem ofClass(ArtifactKey artifact, String className) {
        return new RemovedClassBuildItem(artifact, cn -> cn.equals(className));
    }

    public static RemovedClassBuildItem ofClassAndNestedClasses(ArtifactKey artifact, String className) {
        return new RemovedClassBuildItem(artifact, cn -> cn.equals(className) || cn.startsWith(className.concat("$")));
    }

    private RemovedClassBuildItem(ArtifactKey artifact, Predicate<String> classNamePredicate) {
        this.artifact = artifact;
        this.classNamePredicate = classNamePredicate;
    }

    public ArtifactKey getArtifact() {
        return artifact;
    }

    public Predicate<String> getClassNamePredicate() {
        return classNamePredicate;
    }
}
