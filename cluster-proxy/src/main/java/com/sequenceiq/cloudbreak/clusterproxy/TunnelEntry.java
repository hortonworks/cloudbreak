package com.sequenceiq.cloudbreak.clusterproxy;

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

    @JsonProperty
    private String minaSshdServiceId;

    public TunnelEntry(String key, String serviceType, String host, int port, String minaSshdServiceId) {
        this.key = key;
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
        this.minaSshdServiceId = minaSshdServiceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TunnelEntry that = (TunnelEntry) o;
        return port == that.port &&
                Objects.equals(key, that.key) &&
                Objects.equals(serviceType, that.serviceType) &&
                Objects.equals(host, that.host) &&
                Objects.equals(minaSshdServiceId, that.minaSshdServiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, serviceType, host, port, minaSshdServiceId);
    }

    @Override
    public String toString() {
        return "TunnelEntry{" +
                "key='" + key + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", minaSshdServiceId=" + minaSshdServiceId +
                '}';
    }
}
