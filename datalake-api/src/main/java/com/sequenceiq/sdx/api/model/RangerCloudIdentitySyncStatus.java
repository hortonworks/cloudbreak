package com.sequenceiq.sdx.api.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RangerCloudIdentitySyncStatus {

    @ApiModelProperty(ModelDescriptions.COMMAND_ID)
    private Long commandId;

    @ApiModelProperty(ModelDescriptions.OPERATION_STATUS)
    @NotNull
    private RangerCloudIdentitySyncState state;

    @ApiModelProperty(ModelDescriptions.OPERATION_STATUS_REASON)
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
