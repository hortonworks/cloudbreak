package com.sequenceiq.cloudbreak.telemetry.streaming;

public abstract class CommonStreamingConfiguration {

    private static final int DEFAULT_NUMBER_OF_WORKERS = 1;

    private static final int DEFAULT_QUEUE_SIZE_LIMIT = 2000;

    public boolean isStreamingEnabled() {
        return false;
    }

    public int getNumberOfWorkers() {
        return DEFAULT_NUMBER_OF_WORKERS;
    }

    public int getQueueSizeLimit() {
        return DEFAULT_QUEUE_SIZE_LIMIT;
    }
}
