package com.sequenceiq.sdx.api.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RangerCloudIdentitySyncStatus {

    /**
     * @deprecated use {@link #commandIds}
     */
    @Deprecated
    @Schema(description = ModelDescriptions.COMMAND_ID)
    private Long commandId;

    @NotNull
    @Schema(description = ModelDescriptions.OPERATION_STATUS)
    private RangerCloudIdentitySyncState state;

    @Schema(description = ModelDescriptions.OPERATION_STATUS_REASON)
    private String statusReason;

    @Schema(description = ModelDescriptions.COMMAND_ID)
    private List<Long> commandIds;

    /**
     * @deprecated use {@link #getCommandIds()}
     */
    @Deprecated
    public Long getCommandId() {
        return commandId;
    }

    public RangerCloudIdentitySyncState getState() {
        return state;
    }

    public String getStatusReason() {
        return statusReason;
    }

    /**
     * @deprecated use {@link #setCommandIds(List)}
     */
    @Deprecated
    public void setCommandId(Long commandId) {
        this.commandId = commandId;
    }

    public void setState(RangerCloudIdentitySyncState state) {
        this.state = state;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public List<Long> getCommandIds() {
        return commandIds;
    }

    public void setCommandIds(List<Long> commandIds) {
        this.commandIds = commandIds;
    }

    @Override
    public String toString() {
        return "RangerCloudIdentitySyncStatus{" +
                "commandId=" + commandId +
                ", state=" + state +
                ", statusReason='" + statusReason + '\'' +
                ", commandIds=" + commandIds +
                '}';
    }
}
