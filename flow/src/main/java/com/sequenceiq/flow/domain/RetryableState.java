package com.sequenceiq.flow.domain;

public enum RetryableState {
    FLOW_PENDING,
    LAST_NOT_FAILED_OR_NOT_RETRYABLE,
    NO_SUCCESSFUL_STATE,
    RETRYABLE;

    public boolean isRetryable() {
        return RETRYABLE.equals(this);
    }
}
