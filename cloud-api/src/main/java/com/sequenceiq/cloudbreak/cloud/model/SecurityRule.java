package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Arrays;

public class SecurityRule {

    private final String cidr;
    private final String[] ports;
    private final String protocol;

    public SecurityRule(String cidr, String[] ports, String protocol) {
        this.cidr = cidr;
        this.ports = ports;
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }

    public String[] getPorts() {
        return ports;
    }

    public String getCidr() {
        return cidr;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SecurityRule{");
        sb.append("cidr='").append(cidr).append('\'');
        sb.append(", ports='").append(Arrays.toString(ports)).append('\'');
        sb.append(", protocol='").append(protocol).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
