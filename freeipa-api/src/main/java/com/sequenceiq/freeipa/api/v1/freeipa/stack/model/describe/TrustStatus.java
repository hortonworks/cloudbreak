package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe;

public enum TrustStatus {
    UNKNOWN,
    TRUST_SETUP_REQUIRED,
    TRUST_SETUP_IN_PROGRESS,
    TRUST_SETUP_FAILED,
    TRUST_SETUP_FINISH_REQUIRED,
    TRUST_SETUP_FINISH_IN_PROGRESS,
    TRUST_SETUP_FINISH_FAILED,
    TRUST_ACTIVE,
    TRUST_BROKEN
}
