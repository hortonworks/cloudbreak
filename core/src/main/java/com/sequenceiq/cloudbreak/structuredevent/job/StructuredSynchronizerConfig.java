package com.sequenceiq.cloudbreak.structuredevent.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredSynchronizerConfig {

    @Value("${structuredsynchronizer.enabled:true}")
    private boolean autoSyncEnabled;

    @Value("${structuredsynchronizer.intervalhours:1}")
    private int intervalInHours;

    public boolean isStructuredSyncEnabled() {
        return autoSyncEnabled;
    }

    public int getIntervalInHours() {
        return intervalInHours;
    }
}
