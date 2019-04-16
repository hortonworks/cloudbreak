package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;

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

    public AmbariRepo getAmbariRepo(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(AmbariRepo.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read ambari repo details for stack.", e);
        }
    }

    public StackRepoDetails getHDPRepo(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(StackRepoDetails.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read hdp repo details for stack.", e);
        }
    }

    public Component store(Component component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        Component ret = componentRepository.save(component);
        LOGGER.debug("Component saved: stackId: {}, component: {}", ret.getStack().getId(), ret);
        return ret;
    }

    public List<Component> store(List<Component> components) {
        for (Component component : components) {
            store(component);
        }
        return components;
    }

    public void deleteComponentsForStack(Long stackId) {
        Set<Component> componentsByStackId = componentRepository.findComponentByStackId(stackId);
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
