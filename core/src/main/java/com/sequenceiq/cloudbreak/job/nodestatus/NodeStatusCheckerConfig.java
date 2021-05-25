package com.sequenceiq.cloudbreak.job.nodestatus;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NodeStatusCheckerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusCheckerConfig.class);

    @Value("${nodestatuschecker.intervalsec:1800}")
    private int intervalInSeconds;

    @Value("${nodestatuschecker.enabled:true}")
    private boolean nodeStatusCheckEnabled;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Periodical node status check is {}", nodeStatusCheckEnabled ? "enabled" : "disabled");
    }

    public boolean isNodeStatusCheckEnabled() {
        return nodeStatusCheckEnabled;
    }

    public int getIntervalInSeconds() {
        return intervalInSeconds;
    }
}
