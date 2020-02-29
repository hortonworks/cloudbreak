package com.sequenceiq.environment.network.service;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;

public class Cidrs {

    private final Set<NetworkSubnetRequest> publicSubnets;

    private final Set<NetworkSubnetRequest> privateSubnets;

    private Cidrs(Set<NetworkSubnetRequest> publicSubnets, Set<NetworkSubnetRequest> privateSubnets) {
        this.publicSubnets = publicSubnets;
        this.privateSubnets = privateSubnets;
    }

    public Set<NetworkSubnetRequest> getPublicSubnets() {
        return publicSubnets;
    }

    public Set<NetworkSubnetRequest> getPrivateSubnets() {
        return privateSubnets;
    }

    public static Cidrs cidrs(Set<NetworkSubnetRequest> publicSubnets, Set<NetworkSubnetRequest> privateSubnets) {
        return new Cidrs(publicSubnets, privateSubnets);
    }
}
