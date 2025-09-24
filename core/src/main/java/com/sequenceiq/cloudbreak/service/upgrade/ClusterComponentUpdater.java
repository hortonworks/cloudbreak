package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterComponentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentUpdater.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Inject
    private StackService stackService;

    public void updateClusterComponentsByStackId(Stack stack, Set<Component> targetComponents, boolean removeUnused) {
        Set<ClusterComponent> clusterComponentsFromDb = clusterComponentConfigProvider.getComponentsByClusterId(stack.getCluster().getId());
        targetComponents.forEach(targetComponent -> updateComponentFromDbAttributeField(
                clusterComponentsFromDb,
                targetComponent,
                stack.getCluster())
        );
        clusterComponentConfigProvider.store(clusterComponentsFromDb);
        stackService.updateRuntimeVersion(
                stack.getId(),
                centralCDHVersionCoordinator.calculateStackVersionFromClusterComponents(clusterComponentsFromDb)
        );
        if (removeUnused) {
            removeUnusedComponents(targetComponents, clusterComponentsFromDb);
        }
        LOGGER.info("Updated cluster components:" + clusterComponentsFromDb);
    }

    private Predicate<ClusterComponent> filterDiffBetweenComponentsFromImageAndDatabase(Set<Component> targetComponents) {
        return component -> targetComponents.stream()
                .noneMatch(componentFromImage -> componentFromImage.getName().contains(component.getName()));
    }

    private void updateComponentFromDbAttributeField(Set<ClusterComponent> clusterComponentsFromDb, Component targetComponent, Cluster targetCluster) {
        Optional<ClusterComponent> matchingComponentFromDb = clusterComponentsFromDb.stream()
                .filter(clusterComponent -> isMatchingComponent(targetComponent, clusterComponent, targetCluster))
                .findFirst();
        matchingComponentFromDb.ifPresent(clusterComponentFromDb -> clusterComponentFromDb.setAttributes(targetComponent.getAttributes()));
    }

    private boolean isMatchingComponent(Component component, ClusterComponent clusterComponent, Cluster cluster) {
        return isSameType(component, clusterComponent) &&
                hasSameNameOrCdhComponent(component, clusterComponent) &&
                isIdEqual(clusterComponent, cluster);
    }

    private boolean hasSameNameOrCdhComponent(Component component, ClusterComponent clusterComponent) {
        return isNameEqual(component, clusterComponent) || isCdhComponent(component, clusterComponent);
    }

    private boolean isIdEqual(ClusterComponent clusterComponent, Cluster cluster) {
        return clusterComponent.getCluster().getId().equals(cluster.getId());
    }

    private boolean isCdhComponent(Component component, ClusterComponent clusterComponent) {
        return StackType.CDH.name().equals(clusterComponent.getName()) &&
                StackType.CDH.getComponentType().name().equals(component.getName());
    }

    private boolean isNameEqual(Component component, ClusterComponent clusterComponent) {
        return clusterComponent.getName().equals(component.getName());
    }

    private boolean isSameType(Component component, ClusterComponent clusterComponent) {
        return clusterComponent.getComponentType() == component.getComponentType();
    }

    private void removeUnusedComponents(Set<Component> targetComponents, Set<ClusterComponent> clusterComponentsFromDb) {
        Set<ClusterComponent> unusedCdhProductDetails = clusterComponentsFromDb.stream()
                .filter(component -> centralCDHVersionCoordinator.isCdhProductDetails(component))
                .filter(filterDiffBetweenComponentsFromImageAndDatabase(targetComponents))
                .collect(Collectors.toSet());
        LOGGER.debug("Removing unused components: {}", unusedCdhProductDetails);
        clusterComponentConfigProvider.deleteClusterComponents(unusedCdhProductDetails);
    }

    public void removeUnusedCdhProductsFromClusterComponents(Long clusterId, Set<ClusterComponentView> clusterComponentsByBlueprint,
            ParcelOperationStatus removalStatus) {
        Set<ClusterComponentView> clusterComponentsFromDb = centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(clusterId);
        Set<ClusterComponentView> unusedComponents = getUnusedComponents(clusterComponentsByBlueprint, clusterComponentsFromDb);
        if (unusedComponents.isEmpty()) {
            LOGGER.debug("There is no cluster component to be deleted.");
        } else {
            Set<ClusterComponentView> removedComponents = unusedComponents.stream()
                    .filter(comp -> removalStatus.getSuccessful().containsKey(comp.getName()))
                    .collect(Collectors.toSet());
            clusterComponentConfigProvider.deleteClusterComponentViews(removedComponents);
        }
    }

    private Set<ClusterComponentView> getUnusedComponents(Set<ClusterComponentView> clusterComponentsByBlueprint,
            Set<ClusterComponentView> clusterComponentsFromDb) {
        return clusterComponentsFromDb.stream()
                .filter(clusterComponent -> clusterComponentsByBlueprint.stream()
                        .noneMatch(component -> clusterComponent.getName().equals(component.getName())))
                .collect(Collectors.toSet());
    }
}
