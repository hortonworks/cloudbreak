package com.sequenceiq.cloudbreak.cloud.openstack.view;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class NeutronNetworkView {

    private Network network;

    public NeutronNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetCIDR() {
        return network.getSubnet().getCidr();
    }

    public String getPublicNetId() {
        return network.getParameter("publicNetId", String.class);
    }

}


