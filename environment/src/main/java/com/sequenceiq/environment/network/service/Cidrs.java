package com.sequenceiq.environment.network.service;

import java.util.Set;

public class Cidrs {

    private final Set<String> publicSubnetCidrs;

    private final Set<String> privateSubnetCidrs;

    private Cidrs(Set<String> publicSubnetCidrs, Set<String> privateSubnetCidrs) {
        this.publicSubnetCidrs = publicSubnetCidrs;
        this.privateSubnetCidrs = privateSubnetCidrs;
    }

    public Set<String> getPublicSubnetCidrs() {
        return publicSubnetCidrs;
    }

    public Set<String> getPrivateSubnetCidrs() {
        return privateSubnetCidrs;
    }

    public static Cidrs cidrs(Set<String> publicSubnetCidrs, Set<String> privateSubnetCidrs) {
        return new Cidrs(publicSubnetCidrs, privateSubnetCidrs);
    }
}
