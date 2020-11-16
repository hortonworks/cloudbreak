package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;

public class ImageFilterParams {
    private final Image currentImage;

    private final boolean lockComponents;

    private final Map<String, String> activatedParcels;

    private final boolean checkUpgradeMatrix;

    public ImageFilterParams(Image currentImage, boolean lockComponents, Map<String, String> activatedParcels, boolean checkUpgradeMatrix) {
        this.currentImage = currentImage;
        this.lockComponents = lockComponents;
        this.activatedParcels = activatedParcels;
        this.checkUpgradeMatrix = checkUpgradeMatrix;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public Map<String, String> getActivatedParcels() {
        return activatedParcels;
    }

    public boolean isCheckUpgradeMatrix() {
        return checkUpgradeMatrix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImageFilterParams that = (ImageFilterParams) o;
        return lockComponents == that.lockComponents &&
                checkUpgradeMatrix == that.checkUpgradeMatrix &&
                Objects.equals(currentImage, that.currentImage) &&
                Objects.equals(activatedParcels, that.activatedParcels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentImage, lockComponents, activatedParcels, checkUpgradeMatrix);
    }
}
