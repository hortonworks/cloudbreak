package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.sdx.validation.ValidUpgradeRequest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidUpgradeRequest
public class SdxUpgradeRequest {

    @Schema(description = ModelDescriptions.IMAGE_ID)
    private String imageId;

    @Schema(description = ModelDescriptions.RUNTIME_VERSION)
    private String runtime;

    @Schema(description = ModelDescriptions.LOCK_COMPONENTS)
    private Boolean lockComponents;

    @Schema(description = ModelDescriptions.DRY_RUN)
    private Boolean dryRun;

    @Schema(description = ModelDescriptions.SKIP_BACKUP)
    private Boolean skipBackup;

    @Schema(description = ModelDescriptions.SKIP_DATAHUB_VALIDATION)
    private Boolean skipDataHubValidation;

    @Schema(description = ModelDescriptions.ROLLING_UPGRADE_ENABLED)
    private Boolean rollingUpgradeEnabled;

    @Schema(description = ModelDescriptions.SKIP_VALIDATION)
    private boolean skipValidation;

    @Schema(description = ModelDescriptions.SKIP_ATLAS)
    private boolean skipAtlasMetadata;

    @Schema(description = ModelDescriptions.SKIP_RANGER_AUDIT)
    private boolean skipRangerAudits;

    @Schema(description = ModelDescriptions.SKIP_RANGER_METADATA)
    private boolean skipRangerMetadata;

    @Schema(description = ModelDescriptions.SHOW_AVAILABLE_IMAGES)
    private SdxUpgradeShowAvailableImages showAvailableImages;

    @Schema(description = ModelDescriptions.REPLACE_VMS)
    private SdxUpgradeReplaceVms replaceVms;

    @Schema(description = ModelDescriptions.KEEP_VARIANT)
    private boolean keepVariant;

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

    public boolean isKeepVariant() {
        return keepVariant;
    }

    public void setKeepVariant(boolean keepVariant) {
        this.keepVariant = keepVariant;
    }

    @Schema(hidden = true)
    public boolean isEmpty() {
        return isUnspecifiedUpgradeType() && !Boolean.TRUE.equals(dryRun) && !isShowAvailableImagesSet();
    }

    @Schema(hidden = true)
    public boolean isDryRunOnly() {
        return isUnspecifiedUpgradeType() && Boolean.TRUE.equals(dryRun);
    }

    @Schema(hidden = true)
    public boolean isShowAvailableImagesOnly() {
        return isUnspecifiedUpgradeType() && isShowAvailableImagesSet();
    }

    @Schema(hidden = true)
    public boolean isShowAvailableImagesSet() {
        return Objects.nonNull(showAvailableImages) && SdxUpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) && Objects.isNull(runtime) && !Boolean.TRUE.equals(lockComponents);
    }

    public boolean isSkipValidation() {
        return skipValidation;
    }

    public void setSkipValidation(boolean skipValidation) {
        this.skipValidation = skipValidation;
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
        return "SdxUpgradeRequest{"
                + "imageId='" + imageId + '\''
                + ", runtime='" + runtime + '\''
                + ", lockComponents=" + lockComponents
                + ", dryRun=" + dryRun
                + ", skipBackup=" + skipBackup
                + ", replaceVms=" + replaceVms
                + ", skipDataHubValidation=" + skipDataHubValidation + '}';
    }

    @Override
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public final boolean equals(Object o) {
        if (!(o instanceof SdxUpgradeRequest)) {
            return false;
        }

        SdxUpgradeRequest that = (SdxUpgradeRequest) o;
        return Objects.equals(imageId, that.imageId)
                && Objects.equals(runtime, that.runtime)
                && Objects.equals(lockComponents, that.lockComponents)
                && Objects.equals(dryRun, that.dryRun)
                && Objects.equals(skipBackup, that.skipBackup)
                && Objects.equals(skipDataHubValidation, that.skipDataHubValidation)
                && Objects.equals(rollingUpgradeEnabled, that.rollingUpgradeEnabled)
                && skipValidation == that.skipValidation
                && skipAtlasMetadata == that.skipAtlasMetadata
                && skipRangerAudits == that.skipRangerAudits
                && skipRangerMetadata == that.skipRangerMetadata
                && showAvailableImages == that.showAvailableImages
                && replaceVms == that.replaceVms
                && keepVariant == that.keepVariant;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(imageId);
        result = 31 * result + Objects.hashCode(runtime);
        result = 31 * result + Objects.hashCode(lockComponents);
        result = 31 * result + Objects.hashCode(dryRun);
        result = 31 * result + Objects.hashCode(skipBackup);
        result = 31 * result + Objects.hashCode(skipDataHubValidation);
        result = 31 * result + Objects.hashCode(rollingUpgradeEnabled);
        result = 31 * result + Boolean.hashCode(skipValidation);
        result = 31 * result + Boolean.hashCode(skipAtlasMetadata);
        result = 31 * result + Boolean.hashCode(skipRangerAudits);
        result = 31 * result + Boolean.hashCode(skipRangerMetadata);
        result = 31 * result + Objects.hashCode(showAvailableImages);
        result = 31 * result + Objects.hashCode(replaceVms);
        result = 31 * result + Boolean.hashCode(keepVariant);
        return result;
    }
}
