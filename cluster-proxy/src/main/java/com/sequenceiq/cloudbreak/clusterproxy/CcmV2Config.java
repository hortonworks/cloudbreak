package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CcmV2Config {

    @JsonProperty("backendId")
    private final String backendId;

    @JsonProperty("host")
    private final String gatewayHost;

    @JsonProperty("port")
    private final int gatewayPort;

    @JsonProperty("serviceName")
    private final String serviceName;

    public CcmV2Config(String gatewayHost, int gatewayPort, String backendId, String serviceName) {
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        this.backendId = backendId;
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CcmV2Config.class.getSimpleName() + "[", "]")
                .add("backendId='" + backendId + "'")
                .add("gatewayHost='" + gatewayHost + "'")
                .add("gatewayPort=" + gatewayPort)
                .add("serviceName='" + serviceName + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcmV2Config that = (CcmV2Config) o;
        return gatewayPort == that.gatewayPort &&
                Objects.equals(backendId, that.backendId) &&
                Objects.equals(gatewayHost, that.gatewayHost) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backendId, gatewayHost, gatewayPort, serviceName);
    }
}
