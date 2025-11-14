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

    // Amount of time (in days) after which an unreachable FreeIPA cluster is considered stale during status checks
    @Value("${freeipa.statuschecker.stale.after.days:30}")
    private int staleAfterDays;

    @Value("${freeipa.statuschecker.salt.enabled:false}")
    private boolean saltCheckEnabled;

    @Value("${freeipa.statuschecker.salt.statuschange.enabled:false}")
    private boolean saltCheckStatusChangeEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Status update is {} by auto sync ", updateStatus ? "enabled" : "disabled");
        LOGGER.info("Auto sync is {}", enabled ? "enabled" : "disabled");
        LOGGER.info("Salt check is {}", saltCheckEnabled ? "enabled" : "disabled");
        LOGGER.info("Status change by salt check is {}", saltCheckStatusChangeEnabled ? "enabled" : "disabled");
    }

    public boolean isUpdateStatus() {
        return updateStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getStaleAfterDays() {
        return staleAfterDays;
    }

    public boolean isSaltCheckEnabled() {
        return saltCheckEnabled;
    }

    public boolean isSaltCheckStatusChangeEnabled() {
        return saltCheckStatusChangeEnabled;
    }
}
