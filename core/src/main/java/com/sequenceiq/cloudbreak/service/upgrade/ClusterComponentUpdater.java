package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;

@Service
public class ClusterComponentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentUpdater.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public void updateClusterComponentsByStackId(Stack stack, Set<Component> targetComponents) {
        Set<ClusterComponent> clusterComponentsFromDb = clusterComponentConfigProvider.getComponentsByClusterId(stack.getCluster().getId());
        targetComponents.forEach(targetComponent -> updateComponentFromDbAttributeField(clusterComponentsFromDb, targetComponent));
        clusterComponentConfigProvider.store(clusterComponentsFromDb);
        LOGGER.info("Updated cluster components:" + clusterComponentsFromDb);
    }

    private void updateComponentFromDbAttributeField(Set<ClusterComponent> clusterComponentsFromDb, Component targetComponent) {
        Optional<ClusterComponent> matchingComponentFromDb = clusterComponentsFromDb.stream().
                filter(clusterComponent -> isMatchingComponent(targetComponent, clusterComponent))
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
}
