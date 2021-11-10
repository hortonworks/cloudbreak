package com.sequenceiq.cloudbreak.streaming.config;

public abstract class AbstractStreamingConfiguration {

    private final boolean enabled;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    public AbstractStreamingConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit) {
        this.enabled = enabled;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }
}
