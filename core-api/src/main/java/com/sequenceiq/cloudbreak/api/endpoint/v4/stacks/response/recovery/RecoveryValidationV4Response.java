package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

public class RecoveryValidationV4Response {

    private String reason;

    private RecoveryStatus recoveryStatus;

    public RecoveryValidationV4Response() {
    }

    public RecoveryValidationV4Response(String reason, RecoveryStatus recoveryStatus) {
        this.reason = reason;
        this.recoveryStatus = recoveryStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RecoveryStatus getRecoveryStatus() {
        return recoveryStatus;
    }

    public void setRecoveryStatus(RecoveryStatus recoveryStatus) {
        this.recoveryStatus = recoveryStatus;
    }

    @Override
    public String toString() {
        return "RecoveryValidationV4Response{" +
                "reason='" + reason + '\'' +
                ", recoveryStatus=" + recoveryStatus +
                '}';
    }
}
