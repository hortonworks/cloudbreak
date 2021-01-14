package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database;

public enum DatabaseServerResourceStatus {

    UNKNOWN,
    /**
     * Redbeams is managing / owns the resource.
     */
    SERVICE_MANAGED,
    /**
     * The user is managing / owns the resource.
     */
    USER_MANAGED
}
