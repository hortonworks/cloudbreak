package com.sequenceiq.freeipa.service.freeipa.user.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;

public class UserSyncOptions {

    private final boolean fullSync;

    private final boolean fmsToFreeIpaBatchCallEnabled;

    private final boolean enforceGroupMembershipLimitEnabled;

    private final WorkloadCredentialsUpdateType workloadCredentialsUpdateType;

    private final int largeGroupThreshold;

    private final int largeGroupLimit;

    private final boolean splitFreeIPAUserRetrievalEnabled;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    private UserSyncOptions(boolean fullSync, boolean fmsToFreeIpaBatchCallEnabled,
            WorkloadCredentialsUpdateType workloadCredentialsUpdateType, boolean enforceGroupMembershipLimitEnabled,
            int largeGroupThreshold, int largeGroupLimit, boolean splitFreeIPAUserRetrievalEnabled) {
        checkArgument(workloadCredentialsUpdateType == WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED ||
                workloadCredentialsUpdateType == WorkloadCredentialsUpdateType.FORCE_UPDATE);
        this.fullSync = fullSync;
        this.fmsToFreeIpaBatchCallEnabled = fmsToFreeIpaBatchCallEnabled;
        this.workloadCredentialsUpdateType = workloadCredentialsUpdateType;
        this.enforceGroupMembershipLimitEnabled = enforceGroupMembershipLimitEnabled;
        this.largeGroupThreshold = largeGroupThreshold;
        this.largeGroupLimit = largeGroupLimit;
        this.splitFreeIPAUserRetrievalEnabled = splitFreeIPAUserRetrievalEnabled;
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

    public boolean isEnforceGroupMembershipLimitEnabled() {
        return enforceGroupMembershipLimitEnabled;
    }

    public int getLargeGroupThreshold() {
        return largeGroupThreshold;
    }

    public int getLargeGroupLimit() {
        return largeGroupLimit;
    }

    public boolean isSplitFreeIPAUserRetrievalEnabled() {
        return splitFreeIPAUserRetrievalEnabled;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private boolean fullSync;

        private boolean fmsToFreeIpaBatchCallEnabled;

        private boolean enforceGroupMembershipLimitEnabled;

        private WorkloadCredentialsUpdateType workloadCredentialsUpdateType = WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED;

        private int largeGroupThreshold;

        private int largeGroupLimit;

        private boolean splitFreeIPAUserRetrievalEnabled;

        public Builder fullSync(boolean fullSync) {
            this.fullSync = fullSync;
            return this;
        }

        public Builder fmsToFreeIpaBatchCallEnabled(boolean fmsToFreeIpaBatchCallEnabled) {
            this.fmsToFreeIpaBatchCallEnabled = fmsToFreeIpaBatchCallEnabled;
            return this;
        }

        public Builder enforceGroupMembershipLimitEnabled(boolean enforceGroupMembershipLimitEnabled) {
            this.enforceGroupMembershipLimitEnabled = enforceGroupMembershipLimitEnabled;
            return this;
        }

        public Builder workloadCredentialsUpdateType(WorkloadCredentialsUpdateType workloadCredentialsUpdateType) {
            this.workloadCredentialsUpdateType = workloadCredentialsUpdateType;
            return this;
        }

        public Builder largeGroupThreshold(int largeGroupThreshold) {
            this.largeGroupThreshold = largeGroupThreshold;
            return this;
        }

        public Builder largeGroupLimit(int largeGroupLimit) {
            this.largeGroupLimit = largeGroupLimit;
            return this;
        }

        public Builder splitFreeIPAUserRetrievalEnabled(boolean splitFreeIPAUserRetrievalEnabled) {
            this.splitFreeIPAUserRetrievalEnabled = splitFreeIPAUserRetrievalEnabled;
            return this;
        }

        public UserSyncOptions build() {
            return new UserSyncOptions(fullSync, fmsToFreeIpaBatchCallEnabled, workloadCredentialsUpdateType,
                    enforceGroupMembershipLimitEnabled, largeGroupThreshold, largeGroupLimit, splitFreeIPAUserRetrievalEnabled);
        }
    }
}
