package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkView {

    private static final String VPC = "vpcId";
    private static final String IGW = "internetGatewayId";
    private static final String SUBNET = "subnetId";

    private Network network;

    public AwsNetworkView(Network network) {
        this.network = network;
    }

    public boolean isExistingVPC() {
        return isNoneEmpty(network.getStringParameter(VPC));
    }

    public boolean isExistingSubnet() {
        return isNoneEmpty(network.getStringParameter(SUBNET));
    }

    public boolean isExistingIGW() {
        return isNoneEmpty(network.getStringParameter(IGW));
    }

    public String getExistingSubnet() {
        return network.getStringParameter(SUBNET);
    }

    public boolean isSubnetList() {
        return getExistingSubnet().contains(",");
    }

    public List<String> getSubnetList() {
        return isSubnetList() ? Arrays.asList(getExistingSubnet().split(",")) : Lists.newArrayList(getExistingSubnet());
    }

    public String getExistingIGW() {
        return network.getStringParameter(IGW);
    }

    public String getExistingVPC() {
        return network.getStringParameter(VPC);
    }
}
