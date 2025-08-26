package com.sequenceiq.cloudbreak.quartz.statuscleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StackStatusCleanupConfig {

    @Value("${stackstatuscleanup.intervalseconds:60}")
    private int intervalInSeconds;

    @Value("${stackstatuscleanup.retention.period.days:60}")
    private int retentionPeriodInDays;

    @Value("${stackstatuscleanup.limit:100}")
    private int limit;

    @Value("${stackstatuscleanup.enabled:false}")
    private boolean enabled;

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public int getRetentionPeriodInDays() {
        return retentionPeriodInDays;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
