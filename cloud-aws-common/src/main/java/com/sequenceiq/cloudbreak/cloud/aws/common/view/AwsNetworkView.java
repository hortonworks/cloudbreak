package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.INTERNET_GATEWAY_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class AwsNetworkView {

    public static final String REGION = "region";

    @VisibleForTesting
    static final String VPC_CIDR = "vpcCidr";

    @VisibleForTesting
    static final String VPC_CIDRS = "vpcCidrs";

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
        return isNotEmpty(network.getStringParameter(INTERNET_GATEWAY_ID));
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
        return network.getStringParameter(INTERNET_GATEWAY_ID);
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

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return network.getOutboundInternetTraffic();
    }
}
