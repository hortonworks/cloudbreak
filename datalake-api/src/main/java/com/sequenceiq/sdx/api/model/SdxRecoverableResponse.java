package com.sequenceiq.sdx.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRecoverableResponse {

    @ApiModelProperty(ModelDescriptions.RECOVERABLE_STATUS_REASON)
    private String reason;

    @ApiModelProperty(ModelDescriptions.RECOVERABLE_STATUS)
    private RecoveryStatus status;

    public SdxRecoverableResponse(String reason, RecoveryStatus status) {
        this.reason = reason;
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SdxRecoverableResponse{" +
                "reason='" + reason + '\'' +
                ", status=" + status +
                '}';
    }
}
