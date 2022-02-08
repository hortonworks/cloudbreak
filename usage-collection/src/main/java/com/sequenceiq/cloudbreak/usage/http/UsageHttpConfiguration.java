package com.sequenceiq.cloudbreak.usage.http;

import com.sequenceiq.cloudbreak.streaming.config.AbstractStreamingConfiguration;

public class UsageHttpConfiguration extends AbstractStreamingConfiguration {

    private final String endpoint;

    public UsageHttpConfiguration(boolean enabled, int numberOfWorkers, int queueSizeLimit, String endpoint) {
        super(enabled, numberOfWorkers, queueSizeLimit);
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
