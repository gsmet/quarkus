package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Aggregates all the {@link RemovedClassBuildItem}s for ease of use.
 */
public final class RemovedResourcesBuildItem extends SimpleBuildItem {

    private final Map<ArtifactKey, Set<String>> removedResourcesByArtifactKey;

    public RemovedResourcesBuildItem(Map<ArtifactKey, Set<String>> removedResourcesByArtifactKey) {
        this.removedResourcesByArtifactKey = removedResourcesByArtifactKey.isEmpty() ? Map.of()
                : Collections.unmodifiableMap(removedResourcesByArtifactKey);
    }

    public boolean isEmpty() {
        return removedResourcesByArtifactKey.isEmpty();
    }

    public Set<String> getRemovedResources(ArtifactKey artifactKey) {
        return removedResourcesByArtifactKey.getOrDefault(artifactKey, Set.of());
    }

    public Map<ArtifactKey, Set<String>> getRemovedResources() {
        return removedResourcesByArtifactKey;
    }
}
