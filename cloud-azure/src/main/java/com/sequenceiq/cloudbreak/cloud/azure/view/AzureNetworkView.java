package com.sequenceiq.cloudbreak.cloud.azure.view;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AzureNetworkView {

    @VisibleForTesting
    static final String SUBNETS = "subnets";

    private final Network network;

    public AzureNetworkView(Network network) {
        this.network = network;
    }

    public String getSubnets() {
        return network.getStringParameter(SUBNETS);
    }
}
