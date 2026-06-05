package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ResourceUpdateRequest {

    @NotNull
    @Schema(description = "Resource ID for resource being updated")
    private Long resourceId;

    @Schema(description = "Disk Sync Mode to PERSIST or for DRY_RUN")
    private DiskSyncMode diskSyncMode;

    @NotEmpty
    @Schema(description = "Resource CRN for resource being updated")
    @ResourceCrn
    private String crn;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public DiskSyncMode getDiskSyncMode() {
        return diskSyncMode;
    }

    public void setDiskSyncMode(DiskSyncMode diskSyncMode) {
        this.diskSyncMode = diskSyncMode;
    }
}
