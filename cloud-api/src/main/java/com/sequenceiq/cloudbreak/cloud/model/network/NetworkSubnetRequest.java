package com.sequenceiq.cloudbreak.cloud.model.network;

public class NetworkSubnetRequest {

    private final String cidr;

    private final SubnetType type;

    public NetworkSubnetRequest(String cidr, SubnetType type) {
        this.cidr = cidr;
        this.type = type;
    }

    public String getCidr() {
        return cidr;
    }

    public SubnetType getType() {
        return type;
    }
}
