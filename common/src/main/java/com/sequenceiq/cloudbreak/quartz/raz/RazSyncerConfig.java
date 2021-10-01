package com.sequenceiq.cloudbreak.quartz.raz;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazSyncerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RazSyncerConfig.class);

    @Value("${razsyncer.intervalsec:300}")
    private int intervalInSeconds;

    @Value("${razsyncer.enabled:true}")
    private boolean razSyncEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Raz syncer is {}", razSyncEnabled ? "enabled" : "disabled");
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public void setIntervalInSeconds(int intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }

    public boolean isRazSyncEnabled() {
        return razSyncEnabled;
    }

    public void setRazSyncEnabled(boolean razSyncEnabled) {
        this.razSyncEnabled = razSyncEnabled;
    }
}
