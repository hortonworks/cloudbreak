package com.sequenceiq.cloudbreak.service.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public record UpgradeImageInfo(Image currentImage, StatedImage targetStatedImage) {

    @JsonCreator
    public UpgradeImageInfo(
            @JsonProperty("currentImage") Image currentImage,
            @JsonProperty("targetStatedImage") StatedImage targetStatedImage) {
        this.currentImage = currentImage;
        this.targetStatedImage = targetStatedImage;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
    }
}
