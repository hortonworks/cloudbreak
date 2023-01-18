package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiskModificationRequest implements JsonEntity {

    @NotNull
    @Schema(description = "volumes being updated", required = true)
    private List<Volume> volumesToUpdate;

    @Valid
    @Schema(description = "update request with the desired type and size", required = true)
    private DiskUpdateRequest diskUpdateRequest;

    @NotNull
    @Schema(description = "stack ID being updated", required = true)
    private Long stackId;

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public List<Volume> getVolumesToUpdate() {
        return volumesToUpdate;
    }

    public void setVolumesToUpdate(List<Volume> volumesToUpdate) {
        this.volumesToUpdate = volumesToUpdate;
    }

    public DiskUpdateRequest getDiskUpdateRequest() {
        return diskUpdateRequest;
    }

    public void setDiskUpdateRequest(DiskUpdateRequest diskUpdateRequest) {
        this.diskUpdateRequest = diskUpdateRequest;
    }
}
