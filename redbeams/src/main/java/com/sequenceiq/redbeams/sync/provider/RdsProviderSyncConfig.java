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

    @Value("${redbeams.rds-provider-sync.enabled}")
    private boolean enabled;

    @Value("${redbeams.rds-provider-sync.update}")
    private boolean updateInstanceType;

    @Value("${redbeams.rds-provider-sync.update-version}")
    private boolean updateVersion;

    @Value("${redbeams.rds-provider-sync.interval-in-minutes}")
    private int intervalInMinutes;

    @Value("#{'${redbeams.rds-provider-sync.enabled-providers}'.split(',')}")
    private Set<String> enabledProviders;

    @PostConstruct
    void logStatus() {
        LOGGER.info("RDS provider sync is {}, instance type update is {}, version update is {}, interval is {} minutes, enabled providers: {}",
                enabled ? "enabled" : "disabled", updateInstanceType ? "enabled" : "disabled", updateVersion ? "enabled" : "disabled",
                intervalInMinutes, enabledProviders);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUpdateInstanceType() {
        return updateInstanceType;
    }

    public boolean isUpdateVersion() {
        return updateVersion;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public Set<String> getEnabledProviders() {
        return enabledProviders;
    }
}
