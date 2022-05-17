package com.sequenceiq.freeipa.nodestatus;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeStatusJobConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusJobConfig.class);

    @Value("${freeipa.nodestatus.intervalsec:1800}")
    private int intervalInSeconds;

    @Value("${freeipa.nodestatus.enabled:true}")
    private boolean enabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Periodical node status check is {}", enabled ? "enabled" : "disabled");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }

    public void setIntervalInSeconds(int intervalInSeconds) {
        this.intervalInSeconds = intervalInSeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
