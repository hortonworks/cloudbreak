package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery;

public enum RecoveryStatus {

    RECOVERABLE,
    NON_RECOVERABLE;

    public boolean recoverable() {
        return RECOVERABLE.equals(this);
    }

    public boolean nonRecoverable() {
        return NON_RECOVERABLE.equals(this);
    }
}
