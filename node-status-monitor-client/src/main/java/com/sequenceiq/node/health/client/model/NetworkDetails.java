package com.sequenceiq.node.health.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkDetails {

    private HealthStatus ccmAccessible;

    private Boolean ccmEnabled;

    private HealthStatus clouderaComAccessible;

    private HealthStatus databusAccessible;

    private List<String> dnsResolvers;

    private String host;

    private Long timestamp;

    public HealthStatus getCcmAccessible() {
        return ccmAccessible;
    }

    public void setCcmAccessible(HealthStatus ccmAccessible) {
        this.ccmAccessible = ccmAccessible;
    }

    public Boolean getCcmEnabled() {
        return ccmEnabled;
    }

    public void setCcmEnabled(Boolean ccmEnabled) {
        this.ccmEnabled = ccmEnabled;
    }

    public HealthStatus getClouderaComAccessible() {
        return clouderaComAccessible;
    }

    public void setClouderaComAccessible(HealthStatus clouderaComAccessible) {
        this.clouderaComAccessible = clouderaComAccessible;
    }

    public HealthStatus getDatabusAccessible() {
        return databusAccessible;
    }

    public void setDatabusAccessible(HealthStatus databusAccessible) {
        this.databusAccessible = databusAccessible;
    }

    public List<String> getDnsResolvers() {
        return dnsResolvers;
    }

    public void setDnsResolvers(List<String> dnsResolvers) {
        this.dnsResolvers = dnsResolvers;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NetworkDetails{" +
                "ccmAccessible=" + ccmAccessible +
                ", ccmEnabled=" + ccmEnabled +
                ", clouderaComAccessible=" + clouderaComAccessible +
                ", databusAccessible=" + databusAccessible +
                ", dnsResolvers=" + dnsResolvers +
                ", host='" + host + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
