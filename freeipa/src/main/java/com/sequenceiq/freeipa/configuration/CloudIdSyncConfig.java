package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudIdSyncConfig {

    private static final int MS_IN_SECOND = 1000;

    @Value("${freeipa.cloudidsync.poller.timeoutMs}")
    private long pollerTimeoutMs;

    @Value("${freeipa.cloudidsync.poller.sleepIntervalMs}")
    private long pollerSleepIntervalMs;

    public long getPollerTimeoutMs() {
        return pollerTimeoutMs;
    }

    public long getPollerTimeoutSeconds() {
        return pollerTimeoutMs * MS_IN_SECOND;
    }

    public long getPollerSleepIntervalMs() {
        return pollerSleepIntervalMs;
    }

}
