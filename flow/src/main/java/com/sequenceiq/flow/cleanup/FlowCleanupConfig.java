package com.sequenceiq.flow.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowCleanupConfig {

    @Value("${flowcleanup.purpeflowchain:false}")
    private boolean purgeFlowChain;

    @Value("${flowcleanup.intervalhours:24}")
    private int intervalInHours;

    @Value("${flowcleanup.retention.period.hours:24}")
    private int retentionPeriodInHours;

    @Value("${flowcleanup.retention.period.hours.failed:336}")
    private int retentionPeriodInHoursForFailedFlows;

    public boolean isPurgeFlowChain() {
        return purgeFlowChain;
    }

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getRetentionPeriodInHours() {
        return retentionPeriodInHours;
    }

    public int getRetentionPeriodInHoursForFailedFlows() {
        return retentionPeriodInHoursForFailedFlows;
    }
}
