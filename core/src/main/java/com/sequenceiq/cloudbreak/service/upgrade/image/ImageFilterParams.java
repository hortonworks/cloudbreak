package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

public class ImageFilterParams {

    private final Image currentImage;

    private final boolean lockComponents;

    private final Map<String, String> activatedParcels;

    private final StackType stackType;

    public ImageFilterParams(Image currentImage, boolean lockComponents, Map<String, String> activatedParcels, StackType stackType) {
        this.currentImage = currentImage;
        this.lockComponents = lockComponents;
        this.activatedParcels = activatedParcels;
        this.stackType = stackType;
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

    public StackType getStackType() {
        return stackType;
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
                Objects.equals(currentImage, that.currentImage) &&
                Objects.equals(stackType, that.stackType) &&
                Objects.equals(activatedParcels, that.activatedParcels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentImage, lockComponents, activatedParcels, stackType);
    }
}
