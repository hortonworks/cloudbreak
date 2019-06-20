package com.sequenceiq.cloudbreak.cloud.scheduler;

public enum PollGroup {
    POLLABLE, CANCELLED;

    public boolean isCancelled() {
        return CANCELLED.equals(this);
    }
}
