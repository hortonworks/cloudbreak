package com.sequenceiq.cloudbreak.service.upgrade;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class UpgradeImageInfo {

    private Image currentImage;

    private StatedImage currentStatedImage;

    private StatedImage targetStatedImage;

    public UpgradeImageInfo(Image currentImage, StatedImage currentStatedImage, StatedImage targetStatedImage) {
        this.currentImage = currentImage;
        this.currentStatedImage = currentStatedImage;
        this.targetStatedImage = targetStatedImage;
    }

    public UpgradeImageInfo() {
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
