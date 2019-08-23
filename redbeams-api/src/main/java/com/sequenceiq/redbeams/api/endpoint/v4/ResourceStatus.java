package com.sequenceiq.redbeams.api.endpoint.v4;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ResourceStatus {
    UNKNOWN,
    /**
     * Redbeams is managing / owns the resource.
     */
    SERVICE_MANAGED,
    /**
     * The user is managing / owns the resource.
     */
    USER_MANAGED;

    private static final Set<ResourceStatus> RELEASABLE = Collections.unmodifiableSet(EnumSet.of(SERVICE_MANAGED));

    public static Set<ResourceStatus> getReleasableValues() {
        return RELEASABLE;
    }

    public boolean isReleasable() {
        return RELEASABLE.contains(this);
    }
}
