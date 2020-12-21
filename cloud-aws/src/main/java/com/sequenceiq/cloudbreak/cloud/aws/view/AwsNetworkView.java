package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkView {

    public static final String VPC_ID = "vpcId";

    public static final String REGION = "region";

    @VisibleForTesting
    static final String VPC_CIDR = "vpcCidr";

    @VisibleForTesting
    static final String VPC_CIDRS = "vpcCidrs";

    @VisibleForTesting
    static final String IGW = "internetGatewayId";

    @VisibleForTesting
    static final String SUBNET_ID = "subnetId";

    static final String ENDPOINT_GATEWAY_SUBNET_ID = "endpointGatewaySubnetId";

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

    private String getEndpointGatewaySubnet() {
        return network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID);
    }

    public boolean containsEndpointGatewaySubnet() {
        return isNotEmpty(network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID));
    }

    public boolean isEndpointGatewaySubnetList() {
        return getEndpointGatewaySubnet() != null && getEndpointGatewaySubnet().contains(",");
    }

    public List<String> getEndpointGatewaySubnetList() {
        return isEndpointGatewaySubnetList() ? List.of(getEndpointGatewaySubnet().split(",")) :
            (containsEndpointGatewaySubnet() ?  List.of(getEndpointGatewaySubnet()) : List.of());
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

    public List<String> getExistingVpcCidrs() {
        List<String> cidrs = network.getParameter(VPC_CIDRS, List.class);
        if (cidrs == null || cidrs.isEmpty()) {
            return List.of(getExistingVpcCidr());
        }
        return cidrs;
    }

}
