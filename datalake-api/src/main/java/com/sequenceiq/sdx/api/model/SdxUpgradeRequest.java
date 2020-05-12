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

    private SdxUpgradeReplaceVms replaceVms = SdxUpgradeReplaceVms.DISABLED;

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

    public Boolean isDryRun() {
        return dryRun;
    }

    public boolean isDryRun(SdxUpgradeRequest request) {
        return Boolean.TRUE.equals(request.isDryRun());
    }

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public SdxUpgradeReplaceVms getReplaceVms() {
        return replaceVms;
    }

    public void setReplaceVms(SdxUpgradeReplaceVms replaceVms) {
        this.replaceVms = replaceVms;
    }

    @ApiModelProperty(hidden = true)
    public boolean isEmpty() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents) &&
                !Boolean.TRUE.equals(dryRun);
    }

    @Override
    public String toString() {
        return "SdxUpgradeRequest{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                ", replaceVms=" + replaceVms +
                '}';
    }
}
