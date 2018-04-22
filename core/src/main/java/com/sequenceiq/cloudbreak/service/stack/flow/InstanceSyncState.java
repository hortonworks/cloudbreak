package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum InstanceSyncState {
    DELETED, DELETED_ON_PROVIDER_SIDE, RUNNING, STOPPED, IN_PROGRESS, UNKNOWN;

    public static InstanceSyncState getInstanceSyncState(InstanceStatus instanceStatus) {
        switch (instanceStatus) {
            case IN_PROGRESS:
                return InstanceSyncState.IN_PROGRESS;
            case STARTED:
                return InstanceSyncState.RUNNING;
            case STOPPED:
                return InstanceSyncState.STOPPED;
            case CREATED:
                return InstanceSyncState.RUNNING;
            case FAILED:
                return InstanceSyncState.DELETED;
            case TERMINATED:
                return InstanceSyncState.DELETED_ON_PROVIDER_SIDE;
            default:
                return InstanceSyncState.UNKNOWN;
        }
    }
}
