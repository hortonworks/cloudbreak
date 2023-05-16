package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.Volume;

import io.swagger.annotations.ApiModel;

@ApiModel
public class DiskModificationRequest {

    @NotNull
    private List<Volume> volumesToUpdate;

    @Valid
    private DiskUpdateRequest diskUpdateRequest;

    @NotNull
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
