package com.sequenceiq.cloudbreak.job.disk;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiskSyncConfig {

    @Value("${disk-sync.intervalminutes:360}")
    private int intervalInMinutes;

    @Value("${disk-sync.enabled:true}")
    private boolean enabled;

    @Value("#{'${disk-sync.enabled-providers}'.split(',')}")
    private Set<String> enabledProviders;

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public boolean isDiskSyncEnabled() {
        return enabled;
    }

    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }
}
