package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resource;

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
