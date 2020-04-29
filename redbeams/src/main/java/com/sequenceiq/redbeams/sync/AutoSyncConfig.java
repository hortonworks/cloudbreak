package com.sequenceiq.redbeams.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AutoSyncConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSyncConfig.class);

    @Value("${redbeams.autosync.update.status:true}")
    private boolean updateStatus;

    @Value("${redbeams.autosync.enabled:true}")
    private boolean enabled;

    @PostConstruct
    void logStatus() {
        LOGGER.info("Status update is {} by auto sync ", getStatusString(updateStatus));
        LOGGER.info("Auto sync is {}", getStatusString(enabled));
    }

    public boolean isUpdateStatus() {
        return updateStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private String getStatusString(boolean status) {
        return status ? "enabled" : "disabled";
    }
}
