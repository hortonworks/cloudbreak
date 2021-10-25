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

    @Value("${statuschecker.longintervalsec:7200}")
    private int longIntervalInSeconds;

    @Value("${statuschecker.enabled:true}")
    private boolean autoSyncEnabled;

    @PostConstruct
    void logEnablement() {
        if (autoSyncEnabled) {
            LOGGER.info("Auto sync is enabled. Short sync period is {} sec. Long sync period is {} sec.", intervalInSeconds, longIntervalInSeconds);
        } else {
            LOGGER.info("Auto sync is disabled.");
        }
    }

    public boolean isAutoSyncEnabled() {
        return autoSyncEnabled;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public int getLongIntervalInSeconds() {
        return longIntervalInSeconds;
    }
}
