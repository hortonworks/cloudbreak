package com.sequenceiq.cloudbreak.conf;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix = "cb.limit")
public class LimitConfiguration {

    private static final String UPGRADE_NODE_COUNT_LIMIT_PROPERTY = "upgrade";

    private static final int DEFAULT_UPGRADE_NODE_COUNT_LIMIT = 200;

    private Map<String, Integer> nodeCountLimits;

    public Map<String, Integer> getNodeCountLimits() {
        return nodeCountLimits;
    }

    public void setNodeCountLimits(Map<String, Integer> nodeCountLimits) {
        this.nodeCountLimits = nodeCountLimits;
    }

    public Integer getNodeCountLimit() {
        return nodeCountLimits.entrySet()
                .stream()
                .filter(e -> !UPGRADE_NODE_COUNT_LIMIT_PROPERTY.equals(e.getKey()))
                .map(Entry::getValue)
                .min(Integer::compareTo)
                .orElse(Integer.MAX_VALUE);
    }

    public Integer getUpgradeNodeCountLimit() {
        return nodeCountLimits.getOrDefault(UPGRADE_NODE_COUNT_LIMIT_PROPERTY, DEFAULT_UPGRADE_NODE_COUNT_LIMIT);
    }
}
