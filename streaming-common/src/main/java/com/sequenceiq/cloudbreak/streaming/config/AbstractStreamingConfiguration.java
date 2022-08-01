package com.sequenceiq.cloudbreak.streaming.config;

import com.sequenceiq.cloudbreak.telemetry.streaming.CommonStreamingConfiguration;

public abstract class AbstractStreamingConfiguration extends CommonStreamingConfiguration {

    private final boolean enabled;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    public AbstractStreamingConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit) {
        this.enabled = enabled;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
    }

    public boolean isStreamingEnabled() {
        return enabled;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }
}
