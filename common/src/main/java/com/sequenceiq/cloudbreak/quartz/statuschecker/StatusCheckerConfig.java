package com.sequenceiq.cloudbreak.quartz.statuschecker;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatusCheckerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckerConfig.class);

    @Value("${statuschecker.intervalsec:180}")
    private int intervalInSeconds;

    @Value("${statuschecker.enabled:true}")
    private boolean autoSyncEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Auto sync is {}", autoSyncEnabled ? "enabled" : "disabled");
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }
}
