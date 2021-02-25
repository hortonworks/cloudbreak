package com.sequenceiq.node.health.client.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaltMasterReport {

    private Map<String, HealthStatus> services;

    private Long timestamp;

    public Map<String, HealthStatus> getServices() {
        return services;
    }

    public void setServices(Map<String, HealthStatus> services) {
        this.services = services;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SaltMasterReport{" +
                "services=" + services +
                ", timestamp=" + timestamp +
                '}';
    }
}
