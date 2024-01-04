package com.sequenceiq.cloudbreak.quartz.saltstatuschecker;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("jobs.salt-status-checker")
public class SaltStatusCheckerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStatusCheckerConfig.class);

    private boolean enabled;

    private int intervalInMinutes;

    private int passwordExpiryThresholdInDays;

    @PostConstruct
    void logEnablement() {
        if (enabled) {
            LOGGER.info("Salt status checker is enabled. Job running interval is {} minutes. Password expiry threshold is {} days.",
                    intervalInMinutes, passwordExpiryThresholdInDays);
        } else {
            LOGGER.info("Salt status checker is disabled.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    public void setIntervalInMinutes(int intervalInMinutes) {
        this.intervalInMinutes = intervalInMinutes;
    }

    public int getPasswordExpiryThresholdInDays() {
        return passwordExpiryThresholdInDays;
    }

    public void setPasswordExpiryThresholdInDays(int passwordExpiryThresholdInDays) {
        this.passwordExpiryThresholdInDays = passwordExpiryThresholdInDays;
    }
}
