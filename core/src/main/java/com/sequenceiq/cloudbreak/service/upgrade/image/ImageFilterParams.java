package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.Blueprint;

public class ImageFilterParams {

    private final Image currentImage;

    private final boolean lockComponents;

    private final Map<String, String> stackRelatedParcels;

    private final StackType stackType;

    private final Blueprint blueprint;

    private final Long stackId;

    private final InternalUpgradeSettings internalUpgradeSettings;

    public ImageFilterParams(Image currentImage, boolean lockComponents, Map<String, String> stackRelatedParcels, StackType stackType, Blueprint blueprint,
            Long stackId) {
        this(currentImage, lockComponents, stackRelatedParcels, stackType, blueprint, stackId, new InternalUpgradeSettings());
    }

    public ImageFilterParams(Image currentImage, boolean lockComponents, Map<String, String> stackRelatedParcels, StackType stackType, Blueprint blueprint,
            Long stackId, InternalUpgradeSettings internalUpgradeSettings) {
        this.currentImage = currentImage;
        this.lockComponents = lockComponents;
        this.stackRelatedParcels = stackRelatedParcels;
        this.stackType = stackType;
        this.blueprint = blueprint;
        this.stackId = stackId;
        this.internalUpgradeSettings = internalUpgradeSettings;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public Map<String, String> getStackRelatedParcels() {
        return stackRelatedParcels;
    }

    public StackType getStackType() {
        return stackType;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public Long getStackId() {
        return stackId;
    }

    public boolean isSkipValidations() {
        return internalUpgradeSettings != null && internalUpgradeSettings.isSkipValidations();
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
                Objects.equals(internalUpgradeSettings, that.internalUpgradeSettings) &&
                Objects.equals(currentImage, that.currentImage) &&
                Objects.equals(stackRelatedParcels, that.stackRelatedParcels) &&
                Objects.equals(stackType, that.stackType) &&
                Objects.equals(blueprint, that.blueprint) &&
                Objects.equals(stackId, that.stackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentImage, lockComponents, stackRelatedParcels, stackType, blueprint, stackId, internalUpgradeSettings);
    }
}
