package com.sequenceiq.cloudbreak.cloudwatch.config;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;

public class CloudwatchConfiguration extends AbstractStreamingConfiguration {

    private final String logGroup;

    private final String logStream;

    private final String region;

    private final int maxRetry;

    public CloudwatchConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit, String logGroup, String logStream, String region, int maxRetry) {
        super(enabled, numberOfWorkers, queueSizeLimit);
        this.logGroup = logGroup;
        this.logStream = logStream;
        this.region = region;
        this.maxRetry = maxRetry;
    }

    public String getLogGroup() {
        return logGroup;
    }

    public String getLogStream() {
        return logStream;
    }

    public String getRegion() {
        return region;
    }

    public int getMaxRetry() {
        return maxRetry;
    }
}
