package com.sequenceiq.node.health.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServicesDetails {

    private List<List<String>> freeipaServices;

    private List<List<String>> infraServices;

    private Long timestamp;

    public List<List<String>> getFreeipaServices() {
        return freeipaServices;
    }

    public void setFreeipaServices(List<List<String>> freeipaServices) {
        this.freeipaServices = freeipaServices;
    }

    public List<List<String>> getInfraServices() {
        return infraServices;
    }

    public void setInfraServices(List<List<String>> infraServices) {
        this.infraServices = infraServices;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ServicesDetails{" +
                "freeipaServices=" + freeipaServices +
                ", infraServices=" + infraServices +
                ", timestamp=" + timestamp +
                '}';
    }
}
