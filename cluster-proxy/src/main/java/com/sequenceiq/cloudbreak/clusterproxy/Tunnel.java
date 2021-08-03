package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Tunnel {

    @JsonProperty
    private final String key;

    @JsonProperty
    private final String serviceType;

    @JsonProperty
    private final String host;

    @JsonProperty
    private final int port;

    @JsonCreator
    public Tunnel(String key, String serviceType, String host, int port) {
        this.key = key;
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tunnel tunnel = (Tunnel) o;
        return port == tunnel.port &&
                Objects.equals(key, tunnel.key) &&
                Objects.equals(serviceType, tunnel.serviceType) &&
                Objects.equals(host, tunnel.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, serviceType, host, port);
    }

    @Override
    public String toString() {
        return "Tunnel{" +
                "key='" + key + '\'' +
                ", serviceType='" + serviceType + '\'' +
                ", host=" + host +
                ", port=" + port +
                '}';
    }
}
