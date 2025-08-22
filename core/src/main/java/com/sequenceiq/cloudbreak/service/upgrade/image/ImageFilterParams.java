package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.common.model.ImageCatalogPlatform;

public class ImageFilterParams {

    private final String targetImageId;

    private final Image currentImage;

    private final String imageCatalogName;

    private final boolean lockComponents;

    private final boolean replaceVms;

    private final Map<String, String> stackRelatedParcels;

    private final StackType stackType;

    private final Blueprint blueprint;

    private final Long stackId;

    private final InternalUpgradeSettings internalUpgradeSettings;

    private final ImageCatalogPlatform imageCatalogPlatform;

    private final String cloudPlatform;

    private final String region;

    private final boolean getAllImages;

    public ImageFilterParams(String targetImageId, Image currentImage, String imageCatalogName, boolean lockComponents, boolean replaceVms,
            Map<String, String> stackRelatedParcels, StackType stackType, Blueprint blueprint, Long stackId, InternalUpgradeSettings internalUpgradeSettings,
            ImageCatalogPlatform imageCatalogPlatform, String cloudPlatform, String region, boolean getAllImages) {
        this.targetImageId = targetImageId;
        this.currentImage = currentImage;
        this.imageCatalogName = imageCatalogName;
        this.lockComponents = lockComponents;
        this.replaceVms = replaceVms;
        this.stackRelatedParcels = stackRelatedParcels;
        this.stackType = stackType;
        this.blueprint = blueprint;
        this.stackId = stackId;
        this.internalUpgradeSettings = internalUpgradeSettings;
        this.imageCatalogPlatform = imageCatalogPlatform;
        this.cloudPlatform = cloudPlatform;
        this.region = region;
        this.getAllImages = getAllImages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTargetImageId() {
        return targetImageId;
    }

    public Image getCurrentImage() {
        return currentImage;
    }

    public String getImageCatalogName() {
        return imageCatalogName;
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public boolean isReplaceVms() {
        return replaceVms;
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

    public boolean isRollingUpgradeEnabled() {
        return internalUpgradeSettings != null && internalUpgradeSettings.isRollingUpgradeEnabled();
    }

    public ImageCatalogPlatform getImageCatalogPlatform() {
        return imageCatalogPlatform;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public String getRegion() {
        return region;
    }

    public boolean isGetAllImages() {
        return getAllImages;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
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
                replaceVms == that.replaceVms &&
                Objects.equals(internalUpgradeSettings, that.internalUpgradeSettings) &&
                Objects.equals(currentImage, that.currentImage) &&
                Objects.equals(stackRelatedParcels, that.stackRelatedParcels) &&
                Objects.equals(stackType, that.stackType) &&
                Objects.equals(blueprint, that.blueprint) &&
                Objects.equals(imageCatalogPlatform, that.imageCatalogPlatform) &&
                Objects.equals(stackId, that.stackId) &&
                Objects.equals(cloudPlatform, that.cloudPlatform) &&
                Objects.equals(region, that.region) &&
                Objects.equals(getAllImages, that.getAllImages) &&
                Objects.equals(targetImageId, that.targetImageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentImage, lockComponents, replaceVms, stackRelatedParcels, stackType, blueprint, stackId, internalUpgradeSettings,
                imageCatalogPlatform, cloudPlatform, region, getAllImages, targetImageId);
    }

    public static final class Builder {

        private String targetImageId;

        private Image currentImage;

        private String imageCatalogName;

        private boolean lockComponents;

        private boolean replaceVms;

        private Map<String, String> stackRelatedParcels;

        private StackType stackType;

        private Blueprint blueprint;

        private Long stackId;

        private InternalUpgradeSettings internalUpgradeSettings;

        private ImageCatalogPlatform imageCatalogPlatform;

        private String cloudPlatform;

        private String region;

        private boolean getAllImages;

        private Builder() {
        }

        public Builder withTargetImageId(String targetImageId) {
            this.targetImageId = targetImageId;
            return this;
        }

        public Builder withCurrentImage(Image currentImage) {
            this.currentImage = currentImage;
            return this;
        }

        public Builder withImageCatalogName(String imageCatalogName) {
            this.imageCatalogName = imageCatalogName;
            return this;
        }

        public Builder withLockComponents(boolean lockComponents) {
            this.lockComponents = lockComponents;
            return this;
        }

        public Builder withReplaceVms(boolean replaceVms) {
            this.replaceVms = replaceVms;
            return this;
        }

        public Builder withStackRelatedParcels(Map<String, String> stackRelatedParcels) {
            this.stackRelatedParcels = stackRelatedParcels;
            return this;
        }

        public Builder withStackType(StackType stackType) {
            this.stackType = stackType;
            return this;
        }

        public Builder withBlueprint(Blueprint blueprint) {
            this.blueprint = blueprint;
            return this;
        }

        public Builder withStackId(Long stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder withInternalUpgradeSettings(InternalUpgradeSettings internalUpgradeSettings) {
            this.internalUpgradeSettings = internalUpgradeSettings;
            return this;
        }

        public Builder withImageCatalogPlatform(ImageCatalogPlatform imageCatalogPlatform) {
            this.imageCatalogPlatform = imageCatalogPlatform;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withGetAllImages(boolean getAllImages) {
            this.getAllImages = getAllImages;
            return this;
        }

        public ImageFilterParams build() {
            return new ImageFilterParams(targetImageId, currentImage, imageCatalogName, lockComponents, replaceVms, stackRelatedParcels, stackType, blueprint,
                    stackId, internalUpgradeSettings, imageCatalogPlatform, cloudPlatform, region, getAllImages);
        }
    }
}
