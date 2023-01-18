package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.UpgradeModelDescription;
import com.sequenceiq.cloudbreak.validation.ValidUpgradeRequest;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidUpgradeRequest
public class UpgradeV4Request {

    @Schema(description = UpgradeModelDescription.IMAGE_ID)
    private String imageId;

    @Schema(description = UpgradeModelDescription.RUNTIME)
    private String runtime;

    @Schema(description = UpgradeModelDescription.LOCK_COMPONENTS)
    private Boolean lockComponents;

    @Schema(description = UpgradeModelDescription.DRY_RUN)
    private Boolean dryRun;

    private Boolean replaceVms = Boolean.TRUE;

    @Schema(description = UpgradeModelDescription.SKIP_DATAHUB_VALIDATION)
    private Boolean skipDataHubValidation;

    @Schema(description = UpgradeModelDescription.SHOW_AVAILABLE_IMAGES)
    private UpgradeShowAvailableImages showAvailableImages = UpgradeShowAvailableImages.DO_NOT_SHOW;

    @Schema(hidden = true)
    private InternalUpgradeSettings internalUpgradeSettings;

    @Schema(description = UpgradeModelDescription.KEEP_VARIANT)
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

    public UpgradeShowAvailableImages getShowAvailableImages() {
        return showAvailableImages;
    }

    public void setShowAvailableImages(UpgradeShowAvailableImages showAvailableImages) {
        this.showAvailableImages = showAvailableImages;
    }

    public InternalUpgradeSettings getInternalUpgradeSettings() {
        return internalUpgradeSettings;
    }

    public void setInternalUpgradeSettings(InternalUpgradeSettings internalUpgradeSettings) {
        this.internalUpgradeSettings = internalUpgradeSettings;
    }

    public Boolean getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(Boolean replaceVms) {
        this.replaceVms = replaceVms;
    }

    public Boolean isSkipDataHubValidation() {
        return skipDataHubValidation;
    }

    public void setSkipDataHubValidation(Boolean skipDataHubValidation) {
        this.skipDataHubValidation = skipDataHubValidation;
    }

    public void setKeepVariant(Boolean keepVariant) {
        this.keepVariant = Boolean.TRUE.equals(keepVariant);
    }

    public boolean isKeepVariant() {
        return keepVariant;
    }

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
        return Objects.nonNull(showAvailableImages) && UpgradeShowAvailableImages.DO_NOT_SHOW != showAvailableImages;
    }

    private boolean isUnspecifiedUpgradeType() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpgradeV4Request.class.getSimpleName() + "[", "]")
                .add("imageId='" + imageId + "'")
                .add("runtime='" + runtime + "'")
                .add("lockComponents=" + lockComponents)
                .add("dryRun=" + dryRun)
                .add("replaceVms=" + replaceVms)
                .add("skipDataHubValidation=" + skipDataHubValidation)
                .add("showAvailableImages=" + showAvailableImages)
                .add("internalUpgradeSettings=" + internalUpgradeSettings)
                .add("keepVariant=" + keepVariant)
                .toString();
    }

}
