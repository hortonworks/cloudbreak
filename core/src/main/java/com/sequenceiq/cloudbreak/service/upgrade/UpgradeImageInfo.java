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

    public static Builder builder() {
        return new Builder();
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public StatedImage getTargetStatedImage() {
        return targetStatedImage;
    }

    public static final class Builder {

        private Image currentImage;

        private StatedImage targetStatedImage;

        private Builder() {
        }

        public Builder withCurrentImage(Image currentImage) {
            this.currentImage = currentImage;
            return this;
        }

        public Builder withTargetStatedImage(StatedImage targetStatedImage) {
            this.targetStatedImage = targetStatedImage;
            return this;
        }

        public UpgradeImageInfo build() {
            return new UpgradeImageInfo(currentImage, targetStatedImage);
        }
    }
}
