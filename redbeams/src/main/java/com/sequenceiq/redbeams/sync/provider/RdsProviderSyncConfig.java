package com.sequenceiq.redbeams.sync.provider;

import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RdsProviderSyncConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsProviderSyncConfig.class);

    @Value("${redbeams.rds-provider-sync.enabled:true}")
    private boolean enabled;

    @Value("${redbeams.rds-provider-sync.update:true}")
    private boolean updateInstanceType;

    @Value("${redbeams.rds-provider-sync.interval-in-minutes:1440}")
    private int intervalInMinutes;

    @Value("#{'${redbeams.rds-provider-sync.enabled-providers:AWS,AZURE}'.split(',')}")
    private Set<String> enabledProviders;

    @PostConstruct
    void logStatus() {
        LOGGER.info("RDS provider sync is {}, instance type update is {}, interval is {} minutes, enabled providers: {}",
                enabled ? "enabled" : "disabled", updateInstanceType ? "enabled" : "disabled", intervalInMinutes, enabledProviders);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUpdateInstanceType() {
        return updateInstanceType;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }
}
