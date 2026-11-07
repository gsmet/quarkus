package io.quarkus.bootstrap.app;

import java.util.List;
import java.util.Objects;

import io.quarkus.maven.dependency.ResolvedDependency;

final class ClassLoaderInputs {

    private ClassLoaderInputs() {
    }

    /**
     * Checks whether two lists of resolved dependencies would contribute the same elements to a class loader.
     * <p>
     * We don't rely on {@link ResolvedDependency#equals(Object)} as it compares the workspace modules by identity
     * and the dependencies may originate from different resolutions of the same application model.
     * Only the attributes affecting the class loader content are compared: the artifact coordinates, the flags
     * (e.g. runtime class path, parent first, lesser priority, reloadable) and the resolved paths.
     */
    static boolean sameClassPath(List<ResolvedDependency> dependencies, List<ResolvedDependency> otherDependencies) {
        if (dependencies.size() != otherDependencies.size()) {
            return false;
        }
        for (int i = 0; i < dependencies.size(); i++) {
            ResolvedDependency dependency = dependencies.get(i);
            ResolvedDependency otherDependency = otherDependencies.get(i);
            if (!dependency.getKey().equals(otherDependency.getKey())
                    || !Objects.equals(dependency.getVersion(), otherDependency.getVersion())
                    || dependency.getFlags() != otherDependency.getFlags()
                    || !Objects.equals(dependency.getResolvedPaths(), otherDependency.getResolvedPaths())) {
                return false;
            }
        }
        return true;
    }
}
