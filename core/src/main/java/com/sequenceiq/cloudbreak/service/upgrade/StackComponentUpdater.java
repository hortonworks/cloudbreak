package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class StackComponentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackComponentUpdater.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Inject
    private StackService stackService;

    public Set<Component> updateComponentsByStackId(Stack stack, Set<Component> targetComponents, boolean removeUnused) {
        Set<Component> componentsFromDb = componentConfigProviderService.getComponentsByStackId(stack.getId());
        componentsFromDb.forEach(component -> setComponentIdIfAlreadyExists(targetComponents, component));
        componentConfigProviderService.store(targetComponents);
        stackService.updateRuntimeVersion(stack.getId(), targetComponents);
        LOGGER.info("Updated components: {}", targetComponents);
        if (removeUnused) {
            removeUnusedCdhComponents(componentsFromDb, targetComponents);
        }
        return targetComponents;
    }

    private Predicate<Component> filterDiffBetweenComponentsFromImageAndDatabase(Set<Component> targetComponents) {
        return component -> targetComponents.stream().noneMatch(componentFromImage -> component.getName().equals(componentFromImage.getName()));
    }

    private void setComponentIdIfAlreadyExists(Collection<Component> targetComponents, Component componentFromDb) {
        Optional<Component> matchingComponentFromDb = targetComponents.stream()
                .filter(targetComponent -> isMatchingComponent(componentFromDb, targetComponent))
                .findFirst();
        matchingComponentFromDb.ifPresent(targetComponent -> targetComponent.setId(componentFromDb.getId()));
    }

    private boolean isMatchingComponent(Component component, Component targetComponent) {
        return isSameType(component, targetComponent) &&
                isNameEqual(component, targetComponent) &&
                isStackIdEqual(component, targetComponent);
    }

    private boolean isStackIdEqual(Component component, Component targetComponent) {
        return targetComponent.getStackId().equals(component.getStackId());
    }

    private boolean isNameEqual(Component component, Component targetComponent) {
        return targetComponent.getName().equals(component.getName());
    }

    private boolean isSameType(Component component, Component targetComponent) {
        return targetComponent.getComponentType() == component.getComponentType();
    }

    private void removeUnusedCdhComponents(Set<Component> componentsFromDb, Set<Component> targetComponents) {
        Set<Component> unusedCdhProductDetails = componentsFromDb.stream()
                .filter(component -> centralCDHVersionCoordinator.isCdhProductDetails(component))
                .filter(filterDiffBetweenComponentsFromImageAndDatabase(targetComponents))
                .collect(Collectors.toSet());
        LOGGER.debug("Removing unused Cdh components: {}", unusedCdhProductDetails);
        componentConfigProviderService.deleteComponents(unusedCdhProductDetails);
    }
}
