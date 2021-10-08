package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Service
public class ClusterComponentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentUpdater.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public void updateClusterComponentsByStackId(Stack stack, Set<Component> targetComponents, boolean removeUnused) {
        Set<ClusterComponent> clusterComponentsFromDb = clusterComponentConfigProvider.getComponentsByClusterId(stack.getCluster().getId());
        targetComponents.forEach(targetComponent -> updateComponentFromDbAttributeField(clusterComponentsFromDb, targetComponent));
        clusterComponentConfigProvider.store(clusterComponentsFromDb);
        if (removeUnused) {
            removeUnusedComponents(targetComponents, clusterComponentsFromDb);
        }
        LOGGER.info("Updated cluster components:" + clusterComponentsFromDb);
    }

    private Predicate<ClusterComponent> filterDiffBetweenComponentsFromImageAndDatabase(Set<Component> targetComponents) {
        return component -> targetComponents.stream().noneMatch(componentFromImage -> componentFromImage.getName().contains(component.getName()));
    }

    private void updateComponentFromDbAttributeField(Set<ClusterComponent> clusterComponentsFromDb, Component targetComponent) {
        Optional<ClusterComponent> matchingComponentFromDb = clusterComponentsFromDb.stream()
                .filter(clusterComponent -> isMatchingComponent(targetComponent, clusterComponent))
                .findFirst();
        matchingComponentFromDb.ifPresent(clusterComponentFromDb -> clusterComponentFromDb.setAttributes(targetComponent.getAttributes()));
    }

    private boolean isMatchingComponent(Component component, ClusterComponent clusterComponent) {
        return isSameType(component, clusterComponent) &&
                hasSameNameOrCdhComponent(component, clusterComponent) &&
                isIdEqual(component, clusterComponent);
    }

    private boolean hasSameNameOrCdhComponent(Component component, ClusterComponent clusterComponent) {
        return isNameEqual(component, clusterComponent) || isCdhComponent(component, clusterComponent);
    }

    private boolean isIdEqual(Component component, ClusterComponent clusterComponent) {
        return clusterComponent.getCluster().getId().equals(component.getStack().getCluster().getId());
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
                .filter(component -> ComponentType.CDH_PRODUCT_DETAILS.equals(component.getComponentType()))
                .filter(filterDiffBetweenComponentsFromImageAndDatabase(targetComponents))
                .collect(Collectors.toSet());
        LOGGER.debug("Removing unused components: {}", unusedCdhProductDetails);
        clusterComponentConfigProvider.deleteClusterComponents(unusedCdhProductDetails);
    }

    public void removeUnusedCdhProductsFromClusterComponents(Long clusterId, Set<ClusterComponent> clusterComponentsByBlueprint,
            ParcelOperationStatus removalStatus) {
        Set<ClusterComponent> clusterComponentsFromDb = clusterComponentConfigProvider.getComponentsByClusterId(clusterId);
        Set<ClusterComponent> unusedComponents = getUnusedComponents(clusterComponentsByBlueprint, clusterComponentsFromDb);
        if (unusedComponents.isEmpty()) {
            LOGGER.debug("There is no cluster component to be deleted.");
        } else {
            Set<ClusterComponent> removedComponents = unusedComponents.stream()
                    .filter(comp -> removalStatus.getSuccessful().containsKey(comp.getName()))
                    .collect(Collectors.toSet());
            clusterComponentConfigProvider.deleteClusterComponents(removedComponents);
        }
    }

    private Set<ClusterComponent> getUnusedComponents(Set<ClusterComponent> clusterComponentsByBlueprint, Set<ClusterComponent> clusterComponentsFromDb) {
        return clusterComponentsFromDb.stream()
                .filter(clusterComponent -> ComponentType.CDH_PRODUCT_DETAILS.equals(clusterComponent.getComponentType()))
                .filter(clusterComponent -> clusterComponentsByBlueprint.stream()
                        .noneMatch(component -> clusterComponent.getName().equals(component.getName())))
                .collect(Collectors.toSet());
    }
}
