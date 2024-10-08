package com.sequenceiq.freeipa.sync;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoSyncConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSyncConfig.class);

    @Value("${freeipa.autosync.update.status:true}")
    private boolean updateStatus;

    @Value("${freeipa.autosync.enabled:true}")
    private boolean enabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Status update is {} by auto sync ", updateStatus ? "enabled" : "disabled");
        LOGGER.info("Auto sync is {}", enabled ? "enabled" : "disabled");
    }

    public boolean isUpdateStatus() {
        return updateStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
