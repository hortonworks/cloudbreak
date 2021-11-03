package com.sequenceiq.cloudbreak.job.existingstackfix;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExistingStackFixerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackFixerConfig.class);

    @Value("${existingstackfixer.intervalhours:1}")
    private int intervalInHours;

    @Value("${existingstackfixer.enabled:true}")
    private boolean existingStackFixerEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Existing stack fixer is {}", existingStackFixerEnabled ? "enabled" : "disabled");
    }

    public boolean isExistingStackFixerEnabled() {
        return existingStackFixerEnabled;
    }

    public int getIntervalInHours() {
        return intervalInHours;
    }
}
