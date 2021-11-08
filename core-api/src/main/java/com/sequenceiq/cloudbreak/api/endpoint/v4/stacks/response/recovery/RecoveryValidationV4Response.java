package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus.NON_RECOVERABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryStatus.RECOVERABLE;

import java.util.StringJoiner;

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

    public RecoveryValidationV4Response merge(RecoveryValidationV4Response other) {
        boolean nonRecoverable = status.nonRecoverable();
        boolean otherNonRecoverable = other.getStatus().nonRecoverable();
        RecoveryStatus mergedState = nonRecoverable || otherNonRecoverable ? NON_RECOVERABLE : RECOVERABLE;
        String delimiter = mergedState.recoverable() ? " " : " Next issue: ";
        StringJoiner mergedReason = new StringJoiner(delimiter);
        if (nonRecoverable) {
            mergedReason.add(this.reason);
            if (otherNonRecoverable) {
                mergedReason.add(other.reason);
            }
        } else if (otherNonRecoverable) {
            mergedReason.add(other.reason);
        } else {
            mergedReason.add(this.reason).add(other.reason);
        }
        return new RecoveryValidationV4Response(mergedReason.toString().trim(), mergedState);
    }

    @Override
    public String toString() {
        return "RecoveryValidationV4Response{" +
                "reason='" + reason + '\'' +
                ", status=" + status +
                '}';
    }
}
