package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.sdx.validation.ValidUpgradeRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidUpgradeRequest
public class SdxUpgradeRequest {

    @ApiModelProperty(ModelDescriptions.IMAGE_ID)
    private String imageId;

    @ApiModelProperty(ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    @ApiModelProperty(ModelDescriptions.LOCK_COMPONENTS)
    private Boolean lockComponents;

    @ApiModelProperty(ModelDescriptions.DRY_RUN)
    private Boolean dryRun;

    @ApiModelProperty(ModelDescriptions.SKIP_BACKUP)
    private Boolean skipBackup;

    @ApiModelProperty(ModelDescriptions.SKIP_DATAHUB_VALIDATION)
    private Boolean skipDataHubValidation;

    @ApiModelProperty(ModelDescriptions.ROLLING_UPGRADE_ENABLED)
    private Boolean rollingUpgradeEnabled;

    @ApiModelProperty(ModelDescriptions.SKIP_ATLAS)
    private boolean skipAtlasMetadata;

    @ApiModelProperty(ModelDescriptions.SKIP_RANGER_AUDIT)
    private boolean skipRangerAudits;

    @ApiModelProperty(ModelDescriptions.SKIP_RANGER_METADATA)
    private boolean skipRangerMetadata;

    @ApiModelProperty(ModelDescriptions.SHOW_AVAILABLE_IMAGES)
    private SdxUpgradeShowAvailableImages showAvailableImages;

    @ApiModelProperty(ModelDescriptions.REPLACE_VMS)
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

    public Boolean getSkipBackup() {
        return skipBackup;
    }

    public void setSkipBackup(Boolean skipBackup) {
        this.skipBackup = skipBackup;
    }

    public Boolean getSkipDataHubValidation() {
        return skipDataHubValidation;
    }

    public void setSkipDataHubValidation(Boolean skipDataHubValidation) {
        this.skipDataHubValidation = skipDataHubValidation;
    }

    public Boolean getRollingUpgradeEnabled() {
        return rollingUpgradeEnabled;
    }

    public void setRollingUpgradeEnabled(Boolean rollingUpgradeEnabled) {
        this.rollingUpgradeEnabled = rollingUpgradeEnabled;
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

    public boolean isSkipAtlasMetadata() {
        return skipAtlasMetadata;
    }

    public void setSkipAtlasMetadata(boolean skipAtlasMetadata) {
        this.skipAtlasMetadata = skipAtlasMetadata;
    }

    public boolean isSkipRangerAudits() {
        return skipRangerAudits;
    }

    public void setSkipRangerAudits(boolean skipRangerAudits) {
        this.skipRangerAudits = skipRangerAudits;
    }

    public boolean isSkipRangerMetadata() {
        return skipRangerMetadata;
    }

    public void setSkipRangerMetadata(boolean skipRangerMetadata) {
        this.skipRangerMetadata = skipRangerMetadata;
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
                ", skipDataHubValidation=" + skipDataHubValidation +
                '}';
    }
}
