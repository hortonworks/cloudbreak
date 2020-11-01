package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

public enum ResourceStatus {
    DEFAULT,
    DEFAULT_DELETED,
    USER_MANAGED,
    OUTDATED;

    public boolean isDefault() {
        return DEFAULT.equals(this);
    }

    public boolean isNonDefault() {
        return !DEFAULT.equals(this);
    }
}
