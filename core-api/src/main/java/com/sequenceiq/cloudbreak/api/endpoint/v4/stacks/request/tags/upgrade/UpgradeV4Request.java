package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidUpgradeRequest;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidUpgradeRequest
public class UpgradeV4Request {

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.IMAGE_ID)
    private String imageId;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.RUNTIME)
    private String runtime;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.LOCK_COMPONENTS)
    private Boolean lockComponents;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.DRY_RUN)
    private Boolean dryRun;

    private Boolean replaceVms = Boolean.TRUE;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.SHOW_AVAILABLE_IMAGES)
    private UpgradeShowAvailableImages showAvailableImages = UpgradeShowAvailableImages.DO_NOT_SHOW;

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

    public UpgradeShowAvailableImages getShowAvailableImages() {
        return showAvailableImages;
    }

    public void setShowAvailableImages(UpgradeShowAvailableImages showAvailableImages) {
        this.showAvailableImages = showAvailableImages;
    }

    public Boolean getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(Boolean replaceVms) {
        this.replaceVms = replaceVms;
    }

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
        return Objects.nonNull(showAvailableImages) && UpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents);
    }

    @Override
    public String toString() {
        return "UpgradeV4Request{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                ", showAvailableImages=" + showAvailableImages +
                '}';
    }
}
