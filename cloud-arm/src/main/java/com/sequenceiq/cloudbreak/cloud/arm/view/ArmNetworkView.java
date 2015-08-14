package com.sequenceiq.cloudbreak.cloud.arm.view;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class ArmNetworkView {

    private Network network;

    public ArmNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetCIDR() {
        return network.getSubnet().getCidr();
    }
}