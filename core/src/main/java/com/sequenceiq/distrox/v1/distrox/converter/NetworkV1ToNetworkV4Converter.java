package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.AzureNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.network.NetworkV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperation;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class NetworkV1ToNetworkV4Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperation.class);

    public NetworkV4Request convert(NetworkV1Request network) {
        NetworkV4Request response = new NetworkV4Request();
        response.setAws(getIfNotNull(network.getAws(), this::convert));
        response.setAzure(getIfNotNull(network.getAzure(), this::convert));
        return response;
    }

    public NetworkV4Request convert(EnvironmentNetworkResponse network) {
        NetworkV4Request response = new NetworkV4Request();
        if (!network.getSubnetIds().isEmpty()) {
            LOGGER.info("Subnets are available in the environment network conversion started");
            response.setAws(getIfNotNull(network.getAws(), aws -> convertToAwsNetwork(network)));
            response.setAzure(getIfNotNull(network.getAzure(), azure -> convertToAzureNetwork(network)));
        } else {
            LOGGER.info("No subnet are available in the environment skipping network conversion");
        }
        return response;
    }

    private AzureNetworkV4Parameters convertToAzureNetwork(EnvironmentNetworkResponse source) {
        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();
        response.setNetworkId(source.getAzure().getNetworkId());
        response.setNoFirewallRules(source.getAzure().getNoFirewallRules());
        response.setNoPublicIp(source.getAzure().getNoPublicIp());
        response.setResourceGroupName(source.getAzure().getResourceGroupName());
        response.setSubnetId(source.getSubnetIds().stream().findFirst().orElseThrow(() -> new BadRequestException("No subnet id for this environment")));
        return response;
    }

    private AwsNetworkV4Parameters convertToAwsNetwork(EnvironmentNetworkResponse source) {
        AwsNetworkV4Parameters response = new AwsNetworkV4Parameters();
        response.setSubnetId(source.getSubnetIds().stream().findFirst().orElseThrow(() -> new BadRequestException("No subnet id for this environment")));
        response.setVpcId(source.getAws().getVpcId());
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

    public NetworkV1Request convert(NetworkV4Request network) {
        NetworkV1Request response = new NetworkV1Request();
        response.setAws(getIfNotNull(network.getAws(), this::convert));
        response.setAzure(getIfNotNull(network.getAzure(), this::convert));
        return response;
    }

    private AzureNetworkV1Parameters convert(AzureNetworkV4Parameters source) {
        AzureNetworkV1Parameters response = new AzureNetworkV1Parameters();
        response.setNetworkId(source.getNetworkId());
        response.setNoFirewallRules(source.getNoFirewallRules());
        response.setNoPublicIp(source.getNoPublicIp());
        response.setResourceGroupName(source.getResourceGroupName());
        response.setSubnetId(source.getSubnetId());
        return response;
    }

    private AwsNetworkV1Parameters convert(AwsNetworkV4Parameters source) {
        AwsNetworkV1Parameters response = new AwsNetworkV1Parameters();
        response.setSubnetId(source.getSubnetId());
        response.setVpcId(source.getVpcId());
        response.setInternetGatewayId(source.getInternetGatewayId());
        return response;
    }
}
