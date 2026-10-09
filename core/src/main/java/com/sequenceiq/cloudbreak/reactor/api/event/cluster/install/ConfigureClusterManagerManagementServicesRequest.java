package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;

public class ConfigureClusterManagerManagementServicesRequest extends StackEvent {

    private final Image originalImage;

    private final Image currentImage;

    private final StatedImage targetStatedImage;

    private final ClusterUpgradeProperties clusterUpgradeProperties;

    @JsonCreator
    public ConfigureClusterManagerManagementServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("originalImage") Image originalImage,
            @JsonProperty("currentImage") Image currentImage,
            @JsonProperty("targetStatedImage") StatedImage targetStatedImage,
            @JsonProperty("clusterUpgradeProperties") ClusterUpgradeProperties clusterUpgradeProperties) {
        super(stackId);
        this.originalImage = originalImage;
        this.currentImage = currentImage;
        this.targetStatedImage = targetStatedImage;
        this.clusterUpgradeProperties = clusterUpgradeProperties;
    }

    public ConfigureClusterManagerManagementServicesRequest(Long stackId) {
        this(stackId, null, null, null, null);
    }

    public ClusterUpgradeProperties getClusterUpgradeProperties() {
        return clusterUpgradeProperties;
    }

    public Image getOriginalImage() {
        return originalImage;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
    }

    public Optional<Image> getOriginalImageOpt() {
        return Optional.ofNullable(originalImage);
    }

    public Optional<Image> getCurrentImageOpt() {
        return Optional.ofNullable(currentImage);
    }

    public Optional<StatedImage> getTargetStatedImageOpt() {
        return Optional.ofNullable(targetStatedImage);
    }

    @Override
    public String toString() {
        return "ConfigureClusterManagerManagementServicesRequest{" +
                "originalImage=" + originalImage +
                ", currentImage=" + currentImage +
                ", targetStatedImage=" + targetStatedImage +
                ", clusterUpgradeProperties=" + clusterUpgradeProperties +
                "} " + super.toString();
    }
}
