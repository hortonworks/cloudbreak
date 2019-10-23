package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TunnelEntry {
    @JsonProperty
    private String key;

    @JsonProperty
    private String serviceType;

    @JsonProperty
    private String host;

    @JsonProperty
    private int port;

    public TunnelEntry(String key, String serviceType, String host, int port) {
        this.key = key;
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TunnelEntry that = (TunnelEntry) o;
        return port == that.port &&
                Objects.equals(key, that.key) &&
                Objects.equals(serviceType, that.serviceType) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, serviceType, host, port);
    }

    @Override
    public String toString() {
        return "TunnelEntry{" +
                "key='" + key + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}