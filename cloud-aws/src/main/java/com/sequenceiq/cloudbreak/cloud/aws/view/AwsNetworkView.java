package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkView {

    public static final String VPC_ID = "vpcId";

    @VisibleForTesting
    static final String VPC_CIDR = "vpcCidr";

    @VisibleForTesting
    static final String IGW = "internetGatewayId";

    @VisibleForTesting
    static final String SUBNET_ID = "subnetId";

    private final Network network;

    public AwsNetworkView(Network network) {
        this.network = network;
    }

    public boolean isExistingVPC() {
        return isNotEmpty(network.getStringParameter(VPC_ID));
    }

    public boolean isExistingSubnet() {
        return isNotEmpty(network.getStringParameter(SUBNET_ID));
    }

    public boolean isExistingIGW() {
        return isNotEmpty(network.getStringParameter(IGW));
    }

    public String getExistingSubnet() {
        return network.getStringParameter(SUBNET_ID);
    }

    public boolean isSubnetList() {
        return isExistingSubnet() && getExistingSubnet().contains(",");
    }

    public List<String> getSubnetList() {
        return isSubnetList() ? List.of(getExistingSubnet().split(",")) : (isExistingSubnet() ? List.of(getExistingSubnet()) : List.of());
    }

    public String getExistingIgw() {
        return network.getStringParameter(IGW);
    }

    public String getExistingVpc() {
        return network.getStringParameter(VPC_ID);
    }

    public String getExistingVpcCidr() {
        return network.getStringParameter(VPC_CIDR);
    }
}
