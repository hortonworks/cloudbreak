package com.sequenceiq.flow.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowCleanupConfig {

    @Value("${flowcleanup.intervalhours:24}")
    private int intervalInHours;

    public int getIntervalInHours() {
        return intervalInHours;
    }
}
