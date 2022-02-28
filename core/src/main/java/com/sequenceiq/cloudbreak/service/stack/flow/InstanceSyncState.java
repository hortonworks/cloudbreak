package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum InstanceSyncState {
    DELETED,
    DELETED_ON_PROVIDER_SIDE,
    DELETED_BY_PROVIDER,
    RUNNING,
    STOPPED,
    IN_PROGRESS,
    UNKNOWN,
    ZOMBIE;

    public static InstanceSyncState getInstanceSyncState(InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case IN_PROGRESS:
                return IN_PROGRESS;
            case STARTED:
                return RUNNING;
            case STOPPED:
                return STOPPED;
            case CREATED:
                return RUNNING;
            case FAILED:
                return DELETED;
            case TERMINATED:
                return DELETED_ON_PROVIDER_SIDE;
            case TERMINATED_BY_PROVIDER:
                return DELETED_BY_PROVIDER;
            case ZOMBIE:
                return ZOMBIE;
            default:
                return UNKNOWN;
        }
    }
}
