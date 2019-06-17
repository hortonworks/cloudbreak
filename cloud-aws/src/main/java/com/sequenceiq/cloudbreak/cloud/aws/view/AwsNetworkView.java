package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkView {

    @VisibleForTesting
    static final String VPC = "vpcId";

    @VisibleForTesting
    static final String IGW = "internetGatewayId";

    @VisibleForTesting
    static final String SUBNET = "subnetId";

    private final Network network;

    public AwsNetworkView(Network network) {
        this.network = network;
    }

    public boolean isExistingVPC() {
        return isNotEmpty(network.getStringParameter(VPC));
    }

    public boolean isExistingSubnet() {
        return isNotEmpty(network.getStringParameter(SUBNET));
    }

    public boolean isExistingIGW() {
        return isNotEmpty(network.getStringParameter(IGW));
    }

    public String getExistingSubnet() {
        return network.getStringParameter(SUBNET);
    }

    public boolean isSubnetList() {
        return isExistingSubnet() && getExistingSubnet().contains(",");
    }

    public List<String> getSubnetList() {
        return isSubnetList() ? List.of(getExistingSubnet().split(",")) : (isExistingSubnet() ? List.of(getExistingSubnet()) : List.of());
    }

    public String getExistingIGW() {
        return network.getStringParameter(IGW);
    }

    public String getExistingVPC() {
        return network.getStringParameter(VPC);
    }
}
