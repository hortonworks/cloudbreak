package com.sequenceiq.cloudbreak.structuredevent.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StructuredSynchronizerConfig {

    @Value("${structuredsynchronizer.intervalhours:1}")
    private int intervalInHours;

    public int getIntervalInHours() {
        return intervalInHours;
    }
}
