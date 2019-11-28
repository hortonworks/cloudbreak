package com.sequenceiq.statuschecker.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StatusCheckerProperties {

    @Value("${statuschecker.intervalsec:60}")
    private int intervalInSeconds;

    @Value("${statuschecker.enabled:false}")
    private boolean autoSyncEnabled;

    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }
}
