package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SecurityRule {

    private final String cidr;

    private final PortDefinition[] ports;

    private final String protocol;

    @JsonCreator
    public SecurityRule(
            @JsonProperty("cidr") String cidr,
            @JsonProperty("ports") PortDefinition[] ports,
            @JsonProperty("protocol") String protocol) {

        this.cidr = cidr;
        this.ports = ports;
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public PortDefinition[] getPorts() {
        return ports;
    }

    public String getCidr() {
        return cidr;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SecurityRule{");
        sb.append("cidr='").append(cidr).append('\'');
        sb.append(", ports='").append(Arrays.toString(ports)).append('\'');
        sb.append(", protocol='").append(protocol).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
