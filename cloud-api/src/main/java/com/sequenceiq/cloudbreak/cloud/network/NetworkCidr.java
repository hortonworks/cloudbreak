package com.sequenceiq.cloudbreak.cloud.network;

import java.util.List;

import com.google.common.collect.Lists;

public class NetworkCidr {

    private String cidr;

    private List<String> cidrs;

    public NetworkCidr(String cidr, List<String> cidrs) {
        this.cidr = cidr;
        this.cidrs = cidrs;
    }

    public NetworkCidr(String cidr) {
        this.cidr = cidr;
        this.cidrs = Lists.newArrayList(cidr);
    }

    public String getCidr() {
        return cidr;
    }

    public List<String> getCidrs() {
        return cidrs;
    }

    @Override
    public String toString() {
        return "NetworkCidr{" +
                "cidr='" + cidr + '\'' +
                ", cidrs=" + cidrs +
                '}';
    }
}
