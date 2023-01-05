package com.sequenceiq.periscope.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScalingActivityCleanupConfig {

    @Value("${periscope.scaling-activity.cleanup-event-age.hours:24}")
    private long cleanupDurationHours;

    public long getCleanupDurationHours() {
        return cleanupDurationHours;
    }
}
