package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.component.PreparedImages;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class ClusterComponentUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterComponentUpdateService.class);

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public void updateOrSavePreparedClusterComponent(long stackId, String imageId) throws IOException {
        LOGGER.debug("Saving prepared images to Cluster Component. Stack id: {}", stackId);
        if (StringUtils.isNotEmpty(imageId)) {
            Optional<Long> clusterId = clusterService.findClusterIdByStackId(stackId);
            LOGGER.debug("Cluster id {} is returned for Stack id: {}", clusterId, stackId);
            if (clusterId.isPresent()) {
                ClusterComponent component = clusterComponentConfigProvider.getComponent(clusterId.get(),
                        ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES, ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES.name());
                if (ObjectUtils.isEmpty(component)) {
                    createAndSaveClusterComponent(clusterId.get(), ComponentType.CLUSTER_UPGRADE_PREPARED_IMAGES, imageId);
                } else {
                    updateClusterComponent(component, imageId);
                }
            }
        }
    }

    private void createAndSaveClusterComponent(long clusterId, ComponentType componentType, String imageId) {
        LOGGER.debug("Creating new prepared images to Cluster Component. Cluster id: {}, image id: {}", clusterId, imageId);
        Cluster clusterReference = clusterService.getClusterReference(clusterId);
        ClusterComponent component = new ClusterComponent(componentType,
                new Json(new PreparedImages(List.of(imageId))), clusterReference);
        clusterComponentConfigProvider.store(component);
    }

    private void updateClusterComponent(ClusterComponent component, String imageId) throws IOException {
        LOGGER.debug("Updating prepared images list. Cluster id: {}, image id: {}", component.getCluster().getId(), imageId);
        List<String> preparedImages = component.getAttributes().get(PreparedImages.class).getPreparedImages();
        if (!preparedImages.contains(imageId)) {
            preparedImages.add(imageId);
            component.setAttributes(new Json(new PreparedImages(preparedImages)));
            clusterComponentConfigProvider.store(component);
        }
    }

    public void deleteClusterComponentByComponentTypeAndStackId(long stackId, ComponentType componentType) {
        LOGGER.debug("Deleting Cluster Component for stack id: {} and component type: {}", stackId, componentType);
        Optional<Long> clusterId = clusterService.findClusterIdByStackId(stackId);
        if (clusterId.isPresent()) {
            clusterComponentConfigProvider.deleteClusterComponentByClusterIdAndComponentType(clusterId.get(), componentType);
            LOGGER.debug("Deleted Cluster Component for stack id: {} and component type: {}", stackId, componentType);
        }
    }
}
