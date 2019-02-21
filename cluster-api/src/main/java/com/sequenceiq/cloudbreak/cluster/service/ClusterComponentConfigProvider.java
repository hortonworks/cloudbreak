package com.sequenceiq.cloudbreak.cluster.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.repository.ClusterComponentRepository;
import com.sequenceiq.cloudbreak.repository.ClusterComponentViewRepository;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class ClusterComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentConfigProvider.class);

    @Inject
    private ClusterComponentRepository componentRepository;

    @Inject
    private ClusterComponentViewRepository componentViewRepository;

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType) {
        return getComponent(clusterId, componentType, componentType.name());
    }

    public ClusterComponent getComponent(Long clusterId, ComponentType componentType, String name) {
        return componentRepository.findComponentByClusterIdComponentTypeName(clusterId, componentType, name);
    }

    public ClusterComponentView getComponentView(Long clusterId, ComponentType componentType) {
        return getComponentView(clusterId, componentType, componentType.name());
    }

    public ClusterComponentView getComponentView(Long clusterId, ComponentType componentType, String name) {
        return componentViewRepository.findOneByClusterIdAndComponentTypeAndName(clusterId, componentType, name);
    }

    public StackRepoDetails getHDPRepo(Long clusterId) {
        try {
            ClusterComponentView component = getComponentView(clusterId, ComponentType.HDP_REPO_DETAILS);
            return component == null ? null : component.getAttributes().get(StackRepoDetails.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read HDP repo details.", e);
        }
    }

    public StackRepoDetails getStackRepoDetails(Long clusterId) {
        ClusterComponent component = Optional.ofNullable(getComponent(clusterId, ComponentType.HDP_REPO_DETAILS))
                .orElse(getComponent(clusterId, ComponentType.HDF_REPO_DETAILS));
        try {
            return component.getAttributes().get(StackRepoDetails.class);
        } catch (IOException e) {
            LOGGER.error("Failed to read repo component for cluster: [{}]", clusterId, e);
            throw new CloudbreakServiceException("Failed to read HDP repo details.", e);
        }
    }

    public StackRepoDetails getStackRepo(Iterable<ClusterComponent> clusterComponents) {
        try {
            return Optional.ofNullable(getComponent(Lists.newArrayList(clusterComponents), StackRepoDetails.class, ComponentType.HDP_REPO_DETAILS))
                    .orElse(getComponent(Lists.newArrayList(clusterComponents), StackRepoDetails.class, ComponentType.HDF_REPO_DETAILS));
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to read HDP repo details.", e);
        }
    }

    public AmbariRepo getAmbariRepo(Long clusterId) {
        try {
            ClusterComponent component = getComponent(clusterId, ComponentType.AMBARI_REPO_DETAILS);
            return component == null ? null : component.getAttributes().get(AmbariRepo.class);
        } catch (IOException e) {
            throw new CloudbreakServiceException("Failed to read Ambari repo", e);
        }
    }

    public AmbariRepo getAmbariRepo(Iterable<ClusterComponent> clusterComponents) {
        try {
            return getComponent(Lists.newArrayList(clusterComponents), AmbariRepo.class, ComponentType.AMBARI_REPO_DETAILS);
        } catch (Exception e) {
            throw new CloudbreakServiceException("Failed to read Ambari repo", e);
        }
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

    public List<ClusterComponent> store(Collection<ClusterComponent> components, Cluster cluster) {
        List<ClusterComponent> ret = new ArrayList<>();
        for (ClusterComponent component : components) {
            component.setCluster(cluster);
            ret.add(store(component));
        }
        return ret;
    }

    public Set<ClusterComponent> findByComponentType(ComponentType componentType) {
        return componentRepository.findByComponentType(componentType);
    }
}
