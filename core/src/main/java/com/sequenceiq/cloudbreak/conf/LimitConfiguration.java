package com.sequenceiq.cloudbreak.conf;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@ConfigurationProperties(prefix = "cb.limit")
public class LimitConfiguration {

    private Map<String, Integer> nodeCountLimits;

    public Map<String, Integer> getNodeCountLimits() {
        return nodeCountLimits;
    }

    public void setNodeCountLimits(Map<String, Integer> nodeCountLimits) {
        this.nodeCountLimits = nodeCountLimits;
    }

    public Integer getNodeCountLimit() {
        return nodeCountLimits.values().stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
    }
}
