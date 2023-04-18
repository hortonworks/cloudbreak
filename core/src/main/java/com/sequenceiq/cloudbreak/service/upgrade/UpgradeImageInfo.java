package com.sequenceiq.cloudbreak.service.upgrade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class UpgradeImageInfo {

    private final Image currentImage;

    private final StatedImage currentStatedImage;

    private final StatedImage targetStatedImage;

    @JsonCreator
    public UpgradeImageInfo(
            @JsonProperty("currentImage") Image currentImage,
            @JsonProperty("currentStatedImage") StatedImage currentStatedImage,
            @JsonProperty("targetStatedImage") StatedImage targetStatedImage) {
        this.currentImage = currentImage;
        this.currentStatedImage = currentStatedImage;
        this.targetStatedImage = targetStatedImage;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public StatedImage getCurrentStatedImage() {
        return currentStatedImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
    }

    @Override
    public String toString() {
        return "UpgradeImageInfo{" +
                "currentImage=" + currentImage +
                ", currentStatedImage=" + currentStatedImage +
                ", targetStatedImage=" + targetStatedImage +
                '}';
    }
}
