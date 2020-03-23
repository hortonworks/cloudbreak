package com.sequenceiq.cloudbreak.cluster.service;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
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

    public Set<ClusterComponent> getComponentListByType(Long clusterId, ComponentType componentType) {
        return componentRepository.findComponentsByClusterIdAndComponentType(clusterId, componentType);
    }

    public Set<ClusterComponent> getComponentsByClusterId(Long clusterId) {
        return componentRepository.findComponentByClusterId(clusterId);
    }

    public ClouderaManagerRepo getClouderaManagerRepoDetails(Long clusterId) {
        ClusterComponent component = getComponent(clusterId, ComponentType.CM_REPO_DETAILS);
        return retrieveFromAttribute(component, ClouderaManagerRepo.class);
    }

    public StackRepoDetails getStackRepoDetails(Long clusterId) {
        ClusterComponent component = ofNullable(getComponent(clusterId, ComponentType.HDP_REPO_DETAILS))
                .orElse(getComponent(clusterId, ComponentType.HDF_REPO_DETAILS));
        return retrieveFromAttribute(component, StackRepoDetails.class);
    }

    public List<ClouderaManagerProduct> getClouderaManagerProductDetails(Long clusterId) {
        Set<ClusterComponent> components = getComponentListByType(clusterId, ComponentType.CDH_PRODUCT_DETAILS);
        return components.stream().map(component ->
                retrieveFromAttribute(component, ClouderaManagerProduct.class))
                .collect(Collectors.toList());
    }

    public <T> T getComponent(Collection<ClusterComponent> components, Class<T> clazz, ComponentType componentType) {
        try {
            Optional<ClusterComponent> comp = components.stream().filter(
                    c -> c.getComponentType() == componentType).findFirst();
            return comp.isPresent() ? comp.get().getAttributes().get(clazz) : null;
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read component", e);
        }
    }

    public ClusterComponent store(ClusterComponent component) {
        LOGGER.debug("Component is going to be saved: {}", component);
        ClusterComponent ret = componentRepository.save(component);
        LOGGER.debug("Component saved: stackId: {}, component: {}", ret.getCluster().getId(), ret);
        return ret;
    }

    public Iterable<ClusterComponent> store(Iterable<ClusterComponent> components) {
        return componentRepository.saveAll(components);
    }

    public List<ClusterComponent> store(Collection<ClusterComponent> components, Cluster cluster) {
        List<ClusterComponent> ret = new ArrayList<>();
        components.forEach(comp -> comp.setCluster(cluster));
        componentRepository.saveAll(components).forEach(ret::add);
        LOGGER.debug("Components saved: stackId: {}, components: {}", cluster.getId(), ret);
        return ret;
    }

    private <T> T retrieveFromAttribute(ClusterComponent component, Class<T> clazz) {
        if (component == null) {
            return null;
        }
        return retrieveFromAttributeJson(component.getAttributes(), clazz);
    }

    private <T> T retrieveFromAttributeJson(Json attributes, Class<T> clazz) {
        if (attributes == null) {
            return null;
        }
        try {
            return attributes.get(clazz);
        } catch (IOException e) {
            String message = String.format("Failed to read attributes json into class: %s", clazz);
            LOGGER.debug(message);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
