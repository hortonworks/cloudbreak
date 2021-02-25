package com.sequenceiq.node.health.client.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaltMinionsReport {

    private Map<String, Map<String, HealthStatus>> nodes;

    private Long timestamp;

    public Map<String, Map<String, HealthStatus>> getNodes() {
        return nodes;
    }

    public void setNodes(Map<String, Map<String, HealthStatus>> nodes) {
        this.nodes = nodes;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SaltMinionsReport{" +
                "nodes=" + nodes +
                ", timestamp=" + timestamp +
                '}';
    }
}
