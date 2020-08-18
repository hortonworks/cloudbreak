package com.sequenceiq.environment.environment.v1.converter;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.base.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.MockParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

@Component
public class NetworkRequestToDtoConverter {

    private static final String NETWORK_CONVERT_MESSAGE_TEMPLATE = "Setting up {} network param(s) for environment related dto..";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkRequestToDtoConverter.class);

    public NetworkDto convert(EnvironmentNetworkRequest network) {
        LOGGER.debug("Converting network request to dto");
        NetworkDto.Builder builder = NetworkDto.builder();
        if (network.getAws() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "AWS");
            AwsParams awsParams = new AwsParams();
            awsParams.setVpcId(network.getAws().getVpcId());
            builder.withAws(awsParams);
            builder.withNetworkId(network.getAws().getVpcId());
        }
        if (network.getAzure() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Azure");
            AzureParams azureParams = new AzureParams();
            azureParams.setNetworkId(network.getAzure().getNetworkId());
            azureParams.setNoPublicIp(Boolean.TRUE.equals(network.getAzure().getNoPublicIp()));
            azureParams.setResourceGroupName(network.getAzure().getResourceGroupName());
            builder.withAzure(azureParams);
            builder.withNetworkId(network.getAzure().getNetworkId());
        }
        if (network.getYarn() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Yarn");
            YarnParams yarnParams = new YarnParams();
            yarnParams.setQueue(network.getYarn().getQueue());
            yarnParams.setLifetime(network.getYarn().getLifetime());
            builder.withYarn(yarnParams);
        }
        if (network.getMock() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Mock");
            MockParams mockParams = new MockParams();
            mockParams.setInternetGatewayId(network.getMock().getInternetGatewayId());
            mockParams.setVpcId(network.getMock().getVpcId());
            builder.withMock(mockParams);
            builder.withNetworkId(mockParams.getVpcId());
        }
        if (network.getGcp() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Gcp");
            GcpParams gcpParams = new GcpParams();
            gcpParams.setSharedProjectId(network.getGcp().getSharedProjectId());
            gcpParams.setNetworkId(network.getGcp().getNetworkId());
            gcpParams.setNoFirewallRules(Boolean.TRUE.equals(network.getGcp().getNoFirewallRules()));
            gcpParams.setNoPublicIp(Boolean.TRUE.equals(network.getGcp().getNoPublicIp()));
            builder.withGcp(gcpParams);
            builder.withNetworkId(gcpParams.getNetworkId());
        }
        if (network.getSubnetIds() != null) {
            builder.withSubnetMetas(network.getSubnetIds().stream()
                    .collect(Collectors.toMap(id -> id, id -> new CloudSubnet(id, id))));
        }
        return builder
                .withNetworkCidr(network.getNetworkCidr())
                .withNetworkCidrs(getNetworkCidrs(network))
                .withPrivateSubnetCreation(getPrivateSubnetCreation(network))
                .withServiceEndpointCreation(getServiceEndpointCreation(network))
                .withOutboundInternetTraffic(getOutboundInternetTraffic(network))
                .build();
    }

    private Set<String> getNetworkCidrs(EnvironmentNetworkRequest network) {
        return Strings.isNullOrEmpty(network.getNetworkCidr()) ? null : Set.of(network.getNetworkCidr());
    }

    private PrivateSubnetCreation getPrivateSubnetCreation(EnvironmentNetworkRequest network) {
        return Optional.ofNullable(network.getPrivateSubnetCreation()).orElse(PrivateSubnetCreation.DISABLED);
    }

    private ServiceEndpointCreation getServiceEndpointCreation(EnvironmentNetworkRequest network) {
        return Optional.ofNullable(network.getServiceEndpointCreation()).orElse(ServiceEndpointCreation.DISABLED);
    }

    private OutboundInternetTraffic getOutboundInternetTraffic(EnvironmentNetworkRequest network) {
        return Optional.ofNullable(network.getOutboundInternetTraffic()).orElse(OutboundInternetTraffic.ENABLED);
    }
}
