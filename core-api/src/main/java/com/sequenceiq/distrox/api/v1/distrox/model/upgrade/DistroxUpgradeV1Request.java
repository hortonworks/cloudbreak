package com.sequenceiq.distrox.api.v1.distrox.model.upgrade;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

@ValidUpgradeRequest
public class DistroxUpgradeV1Request {

    private String imageId;

    private String runtime;

    private Boolean lockComponents;

    private Boolean dryRun;

    private DistroxUpgradeShowAvailableImages showAvailableImages;

    private DistroxUpgradeReplaceVms replaceVms;

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

    public DistroxUpgradeShowAvailableImages getShowAvailableImages() {
        return showAvailableImages;
    }

    public void setShowAvailableImages(DistroxUpgradeShowAvailableImages showAvailableImages) {
        this.showAvailableImages = showAvailableImages;
    }

    public DistroxUpgradeReplaceVms getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(DistroxUpgradeReplaceVms replaceVms) {
        this.replaceVms = replaceVms;
    }

    @ApiModelProperty(hidden = true)
    public boolean isEmpty() {
        return isUnspecifiedUpgradeType() &&
                !Boolean.TRUE.equals(dryRun) &&
                !isShowAvailableImagesSet();
    }

    @ApiModelProperty(hidden = true)
    public boolean isDryRunOnly() {
        return isUnspecifiedUpgradeType() &&
                Boolean.TRUE.equals(dryRun);
    }

    @ApiModelProperty(hidden = true)
    public boolean isShowAvailableImagesOnly() {
        return isUnspecifiedUpgradeType() &&
                isShowAvailableImagesSet();
    }

    @ApiModelProperty(hidden = true)
    public boolean isShowAvailableImagesSet() {
        return Objects.nonNull(showAvailableImages) && DistroxUpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents);
    }

    @Override
    public String toString() {
        return "DistroxUpgradeRequest{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                ", replaceVms=" + replaceVms +
                '}';
    }
}
