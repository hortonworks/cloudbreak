package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CcmV2Configs {

    @JsonProperty
    private String agentCrn;

    @JsonProperty
    private String host;

    @JsonProperty
    private int port;

    public CcmV2Configs(String agentCrn, String host, int port) {
        this.agentCrn = agentCrn;
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "CcmV2Configs{agentCrn='" + agentCrn + '\''
                + ", host=" + host
                + ", port=" + port
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CcmV2Configs that = (CcmV2Configs) o;
        return port == that.port &&
                Objects.equals(agentCrn, that.agentCrn) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentCrn, host, port);
    }
}
