package com.sequenceiq.cloudbreak.cloud.gcp.view;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class GcpDatabaseNetworkView {

    private final Network network;

    public GcpDatabaseNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetId() {
        return network.getStringParameter("subnetId");
    }

    public String getAvailabilityZone() {
        return network.getStringParameter("availabilityZone");
    }
}
