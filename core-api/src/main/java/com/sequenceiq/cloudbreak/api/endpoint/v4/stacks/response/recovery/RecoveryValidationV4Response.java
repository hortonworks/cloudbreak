package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

public class RecoveryValidationV4Response {

    private String reason;

    private RecoveryStatus status;

    public RecoveryValidationV4Response() {
    }

    public RecoveryValidationV4Response(String reason, RecoveryStatus status) {
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
        return "RecoveryValidationV4Response{" +
                "reason='" + reason + '\'' +
                ", status=" + status +
                '}';
    }
}
