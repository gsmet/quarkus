package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.model.RemovedClass;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Aggregates all the {@link RemovedClassBuildItem}s for ease of use.
 */
public final class RemovedClassesBuildItem extends SimpleBuildItem {

    private final Map<ArtifactKey, List<RemovedClass>> removedClassesByArtifactKey;

    public RemovedClassesBuildItem(Map<ArtifactKey, List<RemovedClass>> removedClassesByArtifactKey) {
        this.removedClassesByArtifactKey = removedClassesByArtifactKey.isEmpty() ? Map.of()
                : Collections.unmodifiableMap(removedClassesByArtifactKey);
    }

    public boolean isEmpty() {
        return removedClassesByArtifactKey.isEmpty();
    }

    public List<RemovedClass> getRemovedClasses(ArtifactKey artifactKey) {
        return removedClassesByArtifactKey.getOrDefault(artifactKey, List.of());
    }

    public Map<ArtifactKey, List<RemovedClass>> getRemovedClasses() {
        return removedClassesByArtifactKey;
    }
}
