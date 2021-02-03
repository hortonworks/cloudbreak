package com.sequenceiq.cloudbreak.service.stack.flow;

public class SyncConfig {

    private final boolean stackStatusUpdateEnabled;

    private final boolean cmServerRunning;

    public SyncConfig(boolean stackStatusUpdateEnabled, boolean cmServerRunning) {
        this.stackStatusUpdateEnabled = stackStatusUpdateEnabled;
        this.cmServerRunning = cmServerRunning;
    }

    public boolean isStackStatusUpdateEnabled() {
        return stackStatusUpdateEnabled;
    }

    public boolean isCmServerRunning() {
        return cmServerRunning;
    }

}
