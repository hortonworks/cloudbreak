package com.sequenceiq.common.api.type;

public enum CommonStatus {
    REQUESTED,
    CREATED,
    DETACHED,
    TRANSITIONAL;

    public boolean resourceExists() {
        return CREATED.equals(this) || DETACHED.equals(this);
    }
}
