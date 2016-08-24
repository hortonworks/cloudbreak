package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;

@Service
public class ComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentConfigProvider.class);

    @Inject
    private ComponentRepository componentRepository;

    public Component getComponent(Long stackId, ComponentType componentType, String name) {
        return componentRepository.findComponentByStackIdComponentTypeName(stackId, componentType, name);
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

    public HDPRepo getHDPRepo(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(HDPRepo.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read HDP repo details.", e);
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
            throw new CloudbreakServiceException("Failed to read Ambari repo", e);
        }
    }

    public AmbariDatabase getAmbariDatabase(Long stackId) {
        try {
            Component component = getComponent(stackId, ComponentType.AMBARI_DATABASE_DETAILS, ComponentType.AMBARI_DATABASE_DETAILS.name());
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(AmbariDatabase.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Ambari database", e);
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
            componentRepository.delete(componentsByStackId);
            LOGGER.debug("Components({}) have been deleted for stack : {}", componentsByStackId.size(), stackId);
        }
    }
}
