package com.sequenceiq.sdx.api.model;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

public class RangerCloudIdentitySyncStatus {

    @ApiModelProperty
    private Long commandId;

    @ApiModelProperty
    @NotNull
    private RangerCloudIdentitySyncState state;

    @ApiModelProperty
    private String statusReason;

    public Long getCommandId() {
        return commandId;
    }

    public RangerCloudIdentitySyncState getState() {
        return state;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public void setState(RangerCloudIdentitySyncState state) {
        this.state = state;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public String toString() {
        return "RangerCloudIdentitySyncStatus{" +
                ", commandId=" + commandId +
                ", state=" + state +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
}
