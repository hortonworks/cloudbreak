package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

public enum AvailabilityStatus {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE;

    AvailabilityStatus() {
    }

    public Boolean isAvailable() {
        return AVAILABLE.equals(this);
    }
}
