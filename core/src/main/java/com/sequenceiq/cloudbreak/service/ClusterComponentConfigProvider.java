package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;

@Service
public class ClusterComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentConfigProvider.class);

    @Inject
    private ClusterComponentRepository componentRepository;

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType) {
        return getComponent(clusterId, componentType, componentType.name());
    }

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType, String name) {
        return componentRepository.findComponentByClusterIdComponentTypeName(clusterId, componentType, name);
    }

    public StackRepoDetails getHDPRepo(Long clusterId) {
        try {
            ClusterComponent component = getComponent(clusterId, ComponentType.HDP_REPO_DETAILS);
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(StackRepoDetails.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read HDP repo details.", e);
        }
    }

    public AmbariRepo getAmbariRepo(Long clusterId) {
        try {
            ClusterComponent component = getComponent(clusterId, ComponentType.AMBARI_REPO_DETAILS);
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(AmbariRepo.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Ambari repo", e);
        }
    }

    public <T> T getComponent(List<ClusterComponent> components, Class<T> clazz, ComponentType componentType) {
        try {
            Optional<ClusterComponent> comp = components.stream().filter(
                    c -> c.getComponentType() == componentType).findFirst();
            return comp.isPresent() ? comp.get().getAttributes().get(clazz) : null;
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read component", e);
        }
    }

    public AmbariDatabase getAmbariDatabase(Long clusterId) {
        try {
            ClusterComponent component = getComponent(clusterId, ComponentType.AMBARI_DATABASE_DETAILS);
            if (component == null) {
                return null;
            }
            return component.getAttributes().get(AmbariDatabase.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Ambari database", e);
        }
    }

    public ClusterComponent store(ClusterComponent component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        ClusterComponent ret = componentRepository.save(component);
        LOGGER.debug("Component saved: stackId: {}, component: {}", ret.getCluster().getId(), ret);
        return ret;
    }

    public List<ClusterComponent> store(List<ClusterComponent> components, Cluster cluster) {
        for (ClusterComponent component : components) {
            component.setCluster(cluster);
            store(component);
        }
        return components;
    }

    public void deleteComponentsForCluster(Long clusterId) {
        Set<ClusterComponent> componentsByClusterId = componentRepository.findComponentByClusterId(clusterId);
        if (!componentsByClusterId.isEmpty()) {
            LOGGER.debug("Components({}) are going to be deleted for cluster: {}", componentsByClusterId.size(), clusterId);
            componentRepository.delete(componentsByClusterId);
            LOGGER.debug("Components({}) have been deleted for cluster : {}", componentsByClusterId.size(), clusterId);
        }
    }
}
