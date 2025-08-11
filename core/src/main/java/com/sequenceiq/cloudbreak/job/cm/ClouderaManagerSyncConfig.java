package com.sequenceiq.cloudbreak.job.cm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClouderaManagerSyncConfig {

    @Value("${cm-sync.intervalminutes:120}")
    private int intervalInMinutes;

    @Value("${cm-sync.enabled:true}")
    private boolean enabled;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isClouderaManagerSyncEnabled() {
        return enabled;
    }
}