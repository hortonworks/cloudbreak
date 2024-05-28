package com.sequenceiq.cloudbreak.service.stack.flow;

public class SyncConfig {

    private final boolean stackStatusUpdateEnabled;

    private final boolean cmServerRunning;

    private final boolean providerResponseError;

    public SyncConfig(boolean stackStatusUpdateEnabled, boolean cmServerRunning, boolean providerResponseError) {
        this.stackStatusUpdateEnabled = stackStatusUpdateEnabled;
        this.cmServerRunning = cmServerRunning;
        this.providerResponseError = providerResponseError;
    }

    public boolean isStackStatusUpdateEnabled() {
        return stackStatusUpdateEnabled;
    }

    public boolean isCmServerRunning() {
        return cmServerRunning;
    }

    public boolean isProviderResponseError() {
        return providerResponseError;
    }
}
