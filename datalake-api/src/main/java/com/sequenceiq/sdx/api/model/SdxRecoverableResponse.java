package com.sequenceiq.sdx.api.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus;

public class SdxRecoverableResponse {

    private String reason;

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
