package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance;

import java.util.Collection;
import java.util.List;

public enum InstanceStatus {
    REQUESTED,
    CREATED,
    UNREGISTERED,
    REGISTERED,
    DECOMMISSIONED,
    TERMINATED,
    DELETED_ON_PROVIDER_SIDE,
    DELETED_BY_PROVIDER,
    FAILED,
    STOPPED,
    REBOOTING,
    UNREACHABLE;

    public static final Collection<InstanceStatus> AVAILABLE_STATUSES = List.of(CREATED);

    public boolean isAvailable() {
        return AVAILABLE_STATUSES.contains(this);
    }
}
