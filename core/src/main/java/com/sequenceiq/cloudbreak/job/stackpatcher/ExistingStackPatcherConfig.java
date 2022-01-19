package com.sequenceiq.cloudbreak.job.stackpatcher;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExistingStackPatcherConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherConfig.class);

    @Value("${existingstackpatcher.intervalhours}")
    private int intervalInHours;

    @Value("${existingstackpatcher.enabled}")
    private boolean existingStackPatcherEnabled;

    @Value("${existingstackpatcher.maxInitialStartDelayInHours}")
    private int maxInitialStartDelayInHours;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Existing stack patcher is {}", existingStackPatcherEnabled ? "enabled" : "disabled");
    }

    public boolean isExistingStackPatcherEnabled() {
        return existingStackPatcherEnabled;
    }

    public int getIntervalInHours() {
        return intervalInHours;
    }

    public int getMaxInitialStartDelayInHours() {
        return maxInitialStartDelayInHours;
    }
}
