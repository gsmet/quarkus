package io.quarkus.deployment.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.RemovedClass;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RemovedClassBuildItem;
import io.quarkus.deployment.builditem.RemovedClassesBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.RemovedResourcesBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

public class RemovedElementsBuildStep {

    private static final Logger LOG = Logger.getLogger(RemovedElementsBuildStep.class);

    private static final String CLASS_SUFFIX = ".class";

    @BuildStep
    public void handleRemovedResources(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<RemovedResourceBuildItem> removedResourceBuildItems,
            BuildProducer<RemovedResourcesBuildItem> removedResources,
            BuildProducer<RemovedClassBuildItem> removedClasses) {
        if (removedResourceBuildItems.isEmpty()) {
            removedResources.produce(new RemovedResourcesBuildItem(Map.of()));
            return;
        }

        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        Set<ArtifactKey> runtimeDependencies = applicationModel.getRuntimeDependencies().stream().map(d -> d.getKey())
                .collect(Collectors.toCollection(HashSet::new));

        Set<ArtifactKey> unknownArtifacts = new TreeSet<>();
        Map<ArtifactKey, Set<String>> removedResourcesByArtifactKey = new HashMap<>();
        for (RemovedResourceBuildItem removedResourceBuildItem : removedResourceBuildItems) {
            if (!runtimeDependencies.contains(removedResourceBuildItem.getArtifact())) {
                unknownArtifacts.add(removedResourceBuildItem.getArtifact());
                continue;
            }

            Set<String> resources = new HashSet<>();
            for (String resource : resources) {
                // this is for backward compatibility as we used to use RemovedResourceBuildItem to remove classes
                if (resource.endsWith(CLASS_SUFFIX)) {
                    removedClasses.produce(RemovedClassBuildItem.ofClass(removedResourceBuildItem.getArtifact(),
                            resource.replace('/', '.').substring(0, CLASS_SUFFIX.length())));
                    continue;
                }
                resources.add(resource);
            }

            removedResourcesByArtifactKey.computeIfAbsent(removedResourceBuildItem.getArtifact(), k -> new HashSet<>())
                    .addAll(removedResourceBuildItem.getResources());
        }

        if (!unknownArtifacts.isEmpty()) {
            LOG.warnf(
                    "Could not remove configured resources from the following artifacts as they were not found in the model: %s",
                    unknownArtifacts);
        }

        removedResources.produce(new RemovedResourcesBuildItem(removedResourcesByArtifactKey));
    }

    @BuildStep
    public void handleRemovedClasses(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<RemovedClassBuildItem> removedClassBuildItems,
            RemovedResourcesBuildItem removedResources,
            BuildProducer<RemovedClassesBuildItem> removedClasses) {
        if (removedClassBuildItems.isEmpty()) {
            removedClasses.produce(new RemovedClassesBuildItem(Map.of()));
            return;
        }

        ApplicationModel applicationModel = curateOutcomeBuildItem.getApplicationModel();
        Set<ArtifactKey> runtimeDependencies = applicationModel.getRuntimeDependencies().stream().map(d -> d.getKey())
                .collect(Collectors.toCollection(HashSet::new));

        Set<ArtifactKey> unknownArtifacts = new TreeSet<>();
        Map<ArtifactKey, List<RemovedClass>> removedClassesByArtifactKey = new HashMap<>();
        for (RemovedClassBuildItem removedClassBuildItem : removedClassBuildItems) {
            if (!runtimeDependencies.contains(removedClassBuildItem.getArtifact())) {
                unknownArtifacts.add(removedClassBuildItem.getArtifact());
                continue;
            }

            removedClassesByArtifactKey.computeIfAbsent(removedClassBuildItem.getArtifact(), k -> new ArrayList<>())
                    .add(new RemovedClass(removedClassBuildItem.getClassNamePredicate()));
        }

        if (!unknownArtifacts.isEmpty()) {
            LOG.warnf(
                    "Could not remove configured classes from the following artifacts as they were not found in the model: %s",
                    unknownArtifacts);
        }

        ((QuarkusClassLoader) Thread.currentThread().getContextClassLoader()).reset(Map.of(), Map.of(),
                removedClassesByArtifactKey, removedResources.getRemovedResources());

        removedClasses.produce(new RemovedClassesBuildItem(removedClassesByArtifactKey));
    }
}
