package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ConfigureClusterManagerManagementServicesRequest extends StackEvent {

    // TODO CB-33421: Remove once in-flight upgrade flows always carry clusterUpgradeProperties.
    private final Image originalImage;

    // TODO CB-33421: Remove once in-flight upgrade flows always carry clusterUpgradeProperties.
    private final StatedImage targetStatedImage;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ConfigureClusterManagerManagementServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("originalImage") Image originalImage,
            @JsonProperty("targetStatedImage") StatedImage targetStatedImage,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(stackId);
        this.originalImage = originalImage;
        this.targetStatedImage = targetStatedImage;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ConfigureClusterManagerManagementServicesRequest(Long stackId) {
        this(stackId, null, null, null);
    }

    public ConfigureClusterManagerManagementServicesRequest(Long stackId, ClusterUpgradeProperties clusterUpgradeProperties) {
        this(stackId, null, null, clusterUpgradeProperties);
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public Image getOriginalImage() {
        return originalImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
    }

    public Optional<Image> getOriginalImageOpt() {
        return Optional.ofNullable(originalImage);
    }

    public Optional<StatedImage> getTargetStatedImageOpt() {
        return Optional.ofNullable(targetStatedImage);
    }

    @Override
    public String toString() {
        return "ConfigureClusterManagerManagementServicesRequest{" +
                "originalImage=" + originalImage +
                ", targetStatedImage=" + targetStatedImage +
                ", clusterUpgradeProperties=" + clusterUpgradeProperties +
                "} " + super.toString();
    }
}
