package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

public enum DatabaseAvailabilityType {
    NONE,
    NON_HA,
    HA,
    ON_ROOT_VOLUME;

    public boolean isEmbedded() {
        return this == NONE || this == ON_ROOT_VOLUME;
    }
}
