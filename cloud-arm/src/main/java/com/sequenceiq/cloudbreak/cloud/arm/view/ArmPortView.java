package com.sequenceiq.cloudbreak.cloud.arm.view;

public class ArmPortView {

    private final String cidr;
    private final String port;
    private final String protocol;

    public ArmPortView(String cidr, String port, String protocol) {
        this.cidr = cidr;
        this.port = port;
        this.protocol = protocol;
    }

    public String getCidr() {
        return cidr;
    }

    public String getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCapitalProtocol() {
        return protocol.substring(0, 1).toUpperCase() + protocol.substring(1);
    }
}
