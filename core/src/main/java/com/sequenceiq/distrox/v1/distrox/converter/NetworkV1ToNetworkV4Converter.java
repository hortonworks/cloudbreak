package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.ifNotNullF;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;

@Component
public class NetworkV1ToNetworkV4Converter {

    public NetworkV4Request convert(NetworkV1Request network) {
        NetworkV4Request response = new NetworkV4Request();
        response.setAws(ifNotNullF(network.getAws(), this::convert));
        response.setAzure(ifNotNullF(network.getAzure(), this::convert));
        return response;
    }

    private AzureNetworkV4Parameters convert(AzureNetworkV1Parameters source) {
        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();
        response.setNetworkId(source.getNetworkId());
        response.setNoFirewallRules(source.getNoFirewallRules());
        response.setNoPublicIp(source.getNoPublicIp());
        response.setResourceGroupName(source.getResourceGroupName());
        response.setSubnetId(source.getSubnetId());
        return response;
    }

    private AwsNetworkV4Parameters convert(AwsNetworkV1Parameters source) {
        AwsNetworkV4Parameters response = new AwsNetworkV4Parameters();
        response.setSubnetId(source.getSubnetId());
        response.setVpcId(source.getVpcId());
        response.setInternetGatewayId(source.getInternetGatewayId());
        return response;
    }
}
