package com.sequenceiq.freeipa.service.freeipa.user.model;

public class UserSyncOptions {

    private final boolean fullSync;

    private final boolean fmsToFreeIpaBatchCallEnabled;

    private final boolean credentialsUpdateOptimizationEnabled;

    public UserSyncOptions(boolean fullSync, boolean fmsToFreeIpaBatchCallEnabled, boolean credentialsUpdateOptimizationEnabled) {
        this.fullSync = fullSync;
        this.fmsToFreeIpaBatchCallEnabled = fmsToFreeIpaBatchCallEnabled;
        this.credentialsUpdateOptimizationEnabled = credentialsUpdateOptimizationEnabled;
    }

    public boolean fullSync() {
        return fullSync;
    }

    public boolean fmsToFreeIpaBatchCallEnabled() {
        return fmsToFreeIpaBatchCallEnabled;
    }

    public boolean credentialsUpdateOptimizationEnabled() {
        return credentialsUpdateOptimizationEnabled;
    }
}
