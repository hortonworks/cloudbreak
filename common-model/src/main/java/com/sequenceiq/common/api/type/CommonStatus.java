package com.sequenceiq.common.api.type;

public enum CommonStatus {
    REQUESTED,
    CREATED,
    DETACHED,
    FAILED;

    public boolean resourceExists() {
        return CREATED.equals(this)
                || DETACHED.equals(this)
                || REQUESTED.equals(this);
    }
}
