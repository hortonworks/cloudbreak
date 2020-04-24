package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@MutuallyExclusiveNotNull(fieldGroups = {"imageId", "runtime", "lockComponents"},
        message = "Only one of imageId or runtime or lockComponents could be specified in the request!",
        allowAllGroupsNull = true)
public class UpgradeV4Request {

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.IMAGE_ID)
    private String imageId;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.RUNTIME)
    private String runtime;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.LOCK_COMPONENTS)
    private Boolean lockComponents;

    @ApiModelProperty(ModelDescriptions.UpgradeModelDescription.DRY_RUN)
    private Boolean dryRun;

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

    public void setDryRun(Boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isEmpty() {
        return Objects.isNull(imageId) &&
                Objects.isNull(runtime) &&
                !Boolean.TRUE.equals(lockComponents) &&
                !Boolean.TRUE.equals(dryRun);
    }

    @Override
    public String toString() {
        return "UpgradeV4Request{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                '}';
    }
}
