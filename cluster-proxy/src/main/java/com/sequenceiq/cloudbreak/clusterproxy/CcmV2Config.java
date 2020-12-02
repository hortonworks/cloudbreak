package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CcmV2Config {

    @JsonProperty("agentCrn")
    private String agentCrn;

    @JsonProperty("backendId")
    private String backendId;

    @JsonProperty("host")
    private String gatewayHost;

    @JsonProperty("port")
    private int gatewayPort;

    public CcmV2Config(String agentCrn, String backendId, String gatewayHost, int gatewayPort) {
        this.agentCrn = agentCrn;
        this.backendId = backendId;
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
    }

    @Override
    public String toString() {
        return "CcmV2Config{" +
                "agentCrn='" + agentCrn + '\'' +
                ", backendId='" + backendId + '\'' +
                ", gatewayHost='" + gatewayHost + '\'' +
                ", gatewayPort=" + gatewayPort +
                '}';
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
                Objects.equals(agentCrn, that.agentCrn) &&
                Objects.equals(backendId, that.backendId) &&
                Objects.equals(gatewayHost, that.gatewayHost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentCrn, backendId, gatewayHost, gatewayPort);
    }
}
