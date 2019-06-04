package com.sequenceiq.redbeams.api.endpoint.v4;

public enum ResourceStatus {
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
