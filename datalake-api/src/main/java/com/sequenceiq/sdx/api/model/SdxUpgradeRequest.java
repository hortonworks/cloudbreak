package com.sequenceiq.sdx.api.model;

import java.util.Objects;

import com.sequenceiq.cloudbreak.validation.MutuallyExclusiveNotNull;

@MutuallyExclusiveNotNull(fieldGroups = {"imageId", "runtime", "lockComponents"},
        message = "Only one of 'imageId', 'runtime' or 'lockComponents' parameter could be specified in the request!",
        allowAllGroupsNull = true)
public class SdxUpgradeRequest {

    private String imageId;

    private String runtime;

    private Boolean lockComponents;

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
        return "SdxUpgradeRequest{" +
                "imageId='" + imageId + '\'' +
                ", runtime='" + runtime + '\'' +
                ", lockComponents=" + lockComponents +
                ", dryRun=" + dryRun +
                '}';
    }
}
