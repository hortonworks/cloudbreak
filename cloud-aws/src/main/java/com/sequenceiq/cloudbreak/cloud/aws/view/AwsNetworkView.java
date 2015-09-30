package com.sequenceiq.cloudbreak.cloud.aws.view;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkView {

    private Network network;

    public AwsNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetCIDR() {
        return network.getSubnet().getCidr();
    }
}