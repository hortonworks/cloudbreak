package com.sequenceiq.cloudbreak.cloud.model;

public class Subnet {

    private String cidr;

    public Subnet(String cidr) {
        this.cidr = cidr;
    }

    public String getCidr() {
        return cidr;
    }
}
