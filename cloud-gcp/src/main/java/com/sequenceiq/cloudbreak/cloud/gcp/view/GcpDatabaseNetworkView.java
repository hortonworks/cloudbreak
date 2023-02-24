package com.sequenceiq.cloudbreak.cloud.gcp.view;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class GcpDatabaseNetworkView {

    private final Network network;

    public GcpDatabaseNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnetId() {
        return network.getStringParameter(SUBNET_ID);
    }

    public String getAvailabilityZone() {
        return network.getStringParameter("availabilityZone");
    }

    public String getSharedProjectId() {
        return network.getStringParameter("sharedProjectId");
    }

}
