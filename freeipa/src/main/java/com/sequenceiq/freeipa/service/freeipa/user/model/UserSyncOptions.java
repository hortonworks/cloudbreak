package com.sequenceiq.freeipa.service.freeipa.user.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;

public class UserSyncOptions {

    private final boolean fullSync;

    private final boolean fmsToFreeIpaBatchCallEnabled;

    private final WorkloadCredentialsUpdateType workloadCredentialsUpdateType;

    public UserSyncOptions(boolean fullSync, boolean fmsToFreeIpaBatchCallEnabled, WorkloadCredentialsUpdateType workloadCredentialsUpdateType) {
        checkArgument(workloadCredentialsUpdateType == WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED ||
                workloadCredentialsUpdateType == WorkloadCredentialsUpdateType.FORCE_UPDATE);
        this.fullSync = fullSync;
        this.fmsToFreeIpaBatchCallEnabled = fmsToFreeIpaBatchCallEnabled;
        this.workloadCredentialsUpdateType = workloadCredentialsUpdateType;
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public boolean isFmsToFreeIpaBatchCallEnabled() {
        return fmsToFreeIpaBatchCallEnabled;
    }

    public boolean isCredentialsUpdateOptimizationEnabled() {
        return workloadCredentialsUpdateType == WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED;
    }
}
