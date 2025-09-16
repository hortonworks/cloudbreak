package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class ConfigureClusterManagerManagementServicesRequest extends StackEvent {

    private final Image currentImage;

    private final StatedImage targetStatedImage;

    @JsonCreator
    public ConfigureClusterManagerManagementServicesRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("currentImage") Image currentImage,
            @JsonProperty("targetStatedImage") StatedImage targetStatedImage) {
        super(stackId);
        this.currentImage = currentImage;
        this.targetStatedImage = targetStatedImage;
    }

    public ConfigureClusterManagerManagementServicesRequest(Long stackId) {
        super(stackId);
        this.currentImage = null;
        this.targetStatedImage = null;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
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
                "currentImage=" + currentImage +
                ", targetStatedImage=" + targetStatedImage +
                "} " + super.toString();
    }
}
