package com.sequenceiq.flow.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowCleanupConfig {

    @Value("${flowcleanup.intervalhours:24}")
    private int intervalInHours;

    @Value("${flowcleanup.retention.period.hours:24}")
    private int retentionPeriodInHours;

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInHours() {
        return retentionPeriodInHours;
    }
}
