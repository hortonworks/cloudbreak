package com.sequenceiq.node.health.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthReport {

    private List<NodeHealth> nodes;

    private Long timestamp;

    public List<NodeHealth> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeHealth> nodes) {
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
        return "HealthReport{" +
                "nodes=" + nodes +
                ", timestamp=" + timestamp +
                '}';
    }
}
