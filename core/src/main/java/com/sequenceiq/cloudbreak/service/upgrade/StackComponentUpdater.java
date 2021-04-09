package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class StackComponentUpdater {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackComponentUpdater.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public Set<Component> updateComponentsByStackId(Stack stack, StatedImage targetImage, Map<InstanceGroupType, String> userData)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        String imageName = imageService.determineImageName(stack.cloudPlatform(), stack.getRegion(), targetImage.getImage());
        Set<Component> componentsFromDb = componentConfigProviderService.getComponentsByStackId(stack.getId());
        Set<Component> targetComponents = imageService.getComponents(stack, userData, targetImage, imageName);
        componentsFromDb.forEach(component -> setComponentIdIfAlreadyExists(targetComponents, component));
        componentConfigProviderService.store(targetComponents);
        LOGGER.info("Updated components:" + targetComponents);
        removeUnusedComponents(componentsFromDb, targetComponents);
        return targetComponents;
    }

    private Predicate<Component> filterDiffBetweenComponentsFromImageAndDatabase(Set<Component> targetComponents) {
        return component -> targetComponents.stream().noneMatch(componentFromImage -> component.getName().equals(componentFromImage.getName()));
    }

    private void setComponentIdIfAlreadyExists(Collection<Component> targetComponents, Component componentFromDb) {
        Optional<Component> matchingComponentFromDb = targetComponents.stream().filter(targetComponent -> isMatchingComponent(componentFromDb, targetComponent))
                .findFirst();
        matchingComponentFromDb.ifPresent(targetComponent -> targetComponent.setId(componentFromDb.getId()));
    }

    private boolean isMatchingComponent(Component component, Component targetComponent) {
        return isSameType(component, targetComponent) &&
                isNameEqual(component, targetComponent) &&
                isStackIdEqual(component, targetComponent);
    }

    private boolean isStackIdEqual(Component component, Component targetComponent) {
        return targetComponent.getStack().getId().equals(component.getStack().getId());
    }

    private boolean isNameEqual(Component component, Component targetComponent) {
        return targetComponent.getName().equals(component.getName());
    }

    private boolean isSameType(Component component, Component targetComponent) {
        return targetComponent.getComponentType() == component.getComponentType();
    }

    private void removeUnusedComponents(Set<Component> componentsFromDb, Set<Component> targetComponents) {
        Set<Component> unusedCdhProductDetails = componentsFromDb.stream()
                .filter(component -> ComponentType.CDH_PRODUCT_DETAILS.equals(component.getComponentType()))
                .filter(filterDiffBetweenComponentsFromImageAndDatabase(targetComponents))
                .collect(Collectors.toSet());
        componentConfigProviderService.deleteComponents(unusedCdhProductDetails);
    }
}
