package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@ValidUpgradeRequest
public class DistroXUpgradeV1Request {

    private String imageId;

    private String runtime;

    private Boolean lockComponents;

    private Boolean dryRun;

    private Boolean rollingUpgradeEnabled;

    private DistroXUpgradeShowAvailableImages showAvailableImages;

    private DistroXUpgradeReplaceVms replaceVms;

    private boolean disableVariantChange;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public Boolean getLockComponents() {
        return lockComponents;
    }

    public void setLockComponents(Boolean lockComponents) {
        this.lockComponents = lockComponents;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public boolean isDryRun() {
        return Boolean.TRUE.equals(dryRun);
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public Boolean getRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public void setRollingUpgradeEnabled(Boolean rollingUpgradeEnabled) {
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
    }

    public DistroXUpgradeShowAvailableImages getShowAvailableImages() {
        return showAvailableImages;
    }

    public void setShowAvailableImages(DistroXUpgradeShowAvailableImages showAvailableImages) {
        this.showAvailableImages = showAvailableImages;
    }

    public DistroXUpgradeReplaceVms getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(DistroXUpgradeReplaceVms replaceVms) {
        this.replaceVms = replaceVms;
    }

    public void setDisableVariantChange(Boolean disableVariantChange) {
        this.disableVariantChange = Boolean.TRUE.equals(disableVariantChange);
    }

    public boolean isVariantChangeDisabled() {
        return disableVariantChange;
    }

    @Schema(hidden = true)
    public boolean isEmpty() {
        return isUnspecifiedUpgradeType() &&
                !Boolean.TRUE.equals(dryRun) &&
                !isShowAvailableImagesSet();
    }

    @Schema(hidden = true)
    public boolean isDryRunOnly() {
        return isUnspecifiedUpgradeType() &&
                Boolean.TRUE.equals(dryRun);
    }

    @Schema(hidden = true)
    public boolean isShowAvailableImagesOnly() {
        return isUnspecifiedUpgradeType() &&
                isShowAvailableImagesSet();
    }

    @Schema(hidden = true)
    public boolean isShowAvailableImagesSet() {
        return Objects.nonNull(showAvailableImages) && DistroXUpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents);
    }

    @Override
    public String toString() {
        return "DistroXUpgradeRequest{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                ", replaceVms=" + replaceVms +
                ", disableVariantChange=" + disableVariantChange +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof DistroXUpgradeV1Request)) {
            return false;
        }

        DistroXUpgradeV1Request that = (DistroXUpgradeV1Request) o;
        return Objects.equals(imageId, that.imageId)
                && Objects.equals(runtime, that.runtime)
                && Objects.equals(lockComponents, that.lockComponents)
                && Objects.equals(dryRun, that.dryRun)
                && Objects.equals(rollingUpgradeEnabled, that.rollingUpgradeEnabled)
                && showAvailableImages == that.showAvailableImages
                && replaceVms == that.replaceVms
                && disableVariantChange == that.disableVariantChange;
    }

    @Override
    public int hashCode() {
        int result = imageId.hashCode();
        result = 31 * result + Objects.hashCode(runtime);
        result = 31 * result + Objects.hashCode(lockComponents);
        result = 31 * result + Objects.hashCode(dryRun);
        result = 31 * result + rollingUpgradeEnabled.hashCode();
        result = 31 * result + Objects.hashCode(showAvailableImages);
        result = 31 * result + Objects.hashCode(replaceVms);
        result = 31 * result + Boolean.hashCode(disableVariantChange);
        return result;
    }
}
