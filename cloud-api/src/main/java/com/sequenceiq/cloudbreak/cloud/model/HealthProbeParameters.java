package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthProbeParameters {

    private final String path;

    private final int port;

    private final NetworkProtocol protocol;

    private final int interval;

    private final int probeDownThreshold;

    @JsonCreator
    public HealthProbeParameters(
            @JsonProperty("healthCheckPath")String healthCheckPath,
            @JsonProperty("healthCheckPort")int healthCheckPort,
            @JsonProperty("healthCheckProtocol") NetworkProtocol healthCheckProtocol,
            @JsonProperty("healthCheckInterval")int healthCheckInterval,
            @JsonProperty("healthCheckProbeDownThreshold")int healthCheckProbeDownThreshold) {
        this.path = healthCheckPath;
        this.port = healthCheckPort;
        this.protocol = healthCheckProtocol;
        this.interval = healthCheckInterval;
        this.probeDownThreshold = healthCheckProbeDownThreshold;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public NetworkProtocol getProtocol() {
        return protocol;
    }

    public int getInterval() {
        return interval;
    }

    public int getProbeDownThreshold() {
        return probeDownThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HealthProbeParameters that)) {
            return false;
        }
        return port == that.port
                && interval == that.interval
                && Objects.equals(path, that.path)
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(probeDownThreshold, that.probeDownThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, port, protocol, interval, probeDownThreshold);
    }

    @Override
    public String toString() {
        return "HealthProbeParameters{" +
                "path='" + path + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", interval=" + interval +
                ", probeDownThreshold='" + probeDownThreshold + '\'' +
                '}';
    }
}
