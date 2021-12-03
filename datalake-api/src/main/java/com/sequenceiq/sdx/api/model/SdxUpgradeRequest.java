package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.sequenceiq.sdx.validation.ValidUpgradeRequest;

import io.swagger.annotations.ApiModelProperty;

@ValidUpgradeRequest
public class SdxUpgradeRequest {

    private String imageId;

    private String runtime;

    private Boolean lockComponents;

    private Boolean dryRun;

    private Boolean skipBackup;

    private SdxUpgradeShowAvailableImages showAvailableImages;

    private SdxUpgradeReplaceVms replaceVms;

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

    public boolean isBackupSkipped() {
        return Boolean.TRUE.equals(skipBackup);
    }

    public void setSkipBackup(Boolean skipBackup) {
        this.skipBackup = skipBackup;
    }

    public SdxUpgradeShowAvailableImages getShowAvailableImages() {
        return showAvailableImages;
    }

    public void setShowAvailableImages(SdxUpgradeShowAvailableImages showAvailableImages) {
        this.showAvailableImages = showAvailableImages;
    }

    public SdxUpgradeReplaceVms getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(SdxUpgradeReplaceVms replaceVms) {
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
        return Objects.nonNull(showAvailableImages) && SdxUpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents);
    }

    @Override
    public String toString() {
        return "SdxUpgradeRequest{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                ", skipBackup=" + skipBackup +
                ", replaceVms=" + replaceVms +
                '}';
    }
}
