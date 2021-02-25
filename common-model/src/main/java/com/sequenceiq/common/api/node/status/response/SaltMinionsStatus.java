package com.sequenceiq.common.api.node.status.response;

import java.util.Map;

public class SaltMinionsStatus {

    private Map<String, HealthStatus> minions;

    private Long timestamp;

    public Map<String, HealthStatus> getMinions() {
        return minions;
    }

    public void setMinions(Map<String, HealthStatus> minions) {
        this.minions = minions;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SaltMinionsStatus{" +
                "minions=" + minions +
                ", timestamp=" + timestamp +
                '}';
    }
}
