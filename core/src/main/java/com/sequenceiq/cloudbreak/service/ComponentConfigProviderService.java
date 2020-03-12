package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Service
public class ComponentConfigProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentConfigProviderService.class);

    @Inject
    private ComponentRepository componentRepository;

    @Nullable
    public Component getComponent(Long stackId, ComponentType componentType, String name) {
        return componentRepository.findComponentByStackIdComponentTypeName(stackId, componentType, name).orElse(null);
    }

    public Set<Component> getAllComponentsByStackIdAndType(Long stackId, Set<ComponentType> componentTypes) {
        return componentRepository.findComponentByStackIdWithType(stackId, componentTypes);
    }

    public Set<Component> getComponentsByStackId(Long stackId) {
        return componentRepository.findComponentByStackId(stackId);
    }

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        try {
            Component component = getComponent(stackId, ComponentType.IMAGE, ComponentType.IMAGE.name());
            if (component == null) {
                throw new CloudbreakImageNotFoundException(String.format("Image not found: stackId: %d, componentType: %s, name: %s",
                    stackId, ComponentType.IMAGE.name(), ComponentType.IMAGE.name()));
            }
            LOGGER.debug("Image found! stackId: {}, component: {}", stackId, component);
            return component.getAttributes().get(Image.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read image", e);
        }
    }

    public Telemetry getTelemetry(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.TELEMETRY, ComponentType.TELEMETRY.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(Telemetry.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Telemetry for stack.", e);
        }
    }

    public CloudbreakDetails getCloudbreakDetails(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.CLOUDBREAK_DETAILS, ComponentType.CLOUDBREAK_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(CloudbreakDetails.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Cloudbreak details for stack.", e);
        }
    }

    public StackTemplate getStackTemplate(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.STACK_TEMPLATE, ComponentType.STACK_TEMPLATE.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(StackTemplate.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read template for stack.", e);
        }
    }

    public Component store(Component component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        Component ret = componentRepository.save(component);
        LOGGER.debug("Component saved: stackId: {}, component: {}", ret.getStack().getId(), ret);
        return ret;
    }

    public Iterable<Component> store(Iterable<Component> components) {
        componentRepository.saveAll(components);
        LOGGER.debug("Components saved: {}", components);
        return components;
    }

    public void deleteComponentsForStack(Long stackId) {
        Set<Component> componentsByStackId = getComponentsByStackId(stackId);
        if (!componentsByStackId.isEmpty()) {
            LOGGER.debug("Components({}) are going to be deleted for stack: {}", componentsByStackId.size(), stackId);
            componentRepository.deleteAll(componentsByStackId);
            LOGGER.debug("Components({}) have been deleted for stack : {}", componentsByStackId.size(), stackId);
        }
    }

    public void replaceImageComponentWithNew(Component component) {
        Component componentEntity = componentRepository.findComponentByStackIdComponentTypeName(component.getStack().getId(), component.getComponentType(),
                component.getName()).orElseThrow(NotFoundException.notFound("component", component.getName()));
        componentEntity.setAttributes(component.getAttributes());
        componentEntity.setName(component.getName());
        componentRepository.save(componentEntity);
    }
}
