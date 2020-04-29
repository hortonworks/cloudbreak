package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;

@Component
public class NetworkDtoToResponseConverter {

    private final SubnetIdProvider subnetIdProvider;

    public NetworkDtoToResponseConverter(SubnetIdProvider subnetIdProvider) {
        this.subnetIdProvider = subnetIdProvider;
    }

    public EnvironmentNetworkResponse convert(NetworkDto network, Tunnel tunnel) {
        return EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                .withCrn(network.getResourceCrn())
                .withSubnetIds(network.getSubnetIds())
                .withNetworkCidr(network.getNetworkCidr())
                .withSubnetMetas(network.getSubnetMetas())
                .withCbSubnets(network.getCbSubnets())
                .withDwxSubnets(network.getDwxSubnets())
                .withMlxSubnets(network.getMlxSubnets())
                .withPreferedSubnetId(subnetIdProvider.provide(network, tunnel, network.getCloudPlatform()))
                .withPrivateSubnetCreation(network.getPrivateSubnetCreation())
                .withExistingNetwork(RegistrationType.EXISTING == network.getRegistrationType())
                .withAws(getIfNotNull(network.getAws(), p -> EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder
                        .anEnvironmentNetworkAwsParams()
                        .withVpcId(p.getVpcId())
                        .build()))
                .withAzure(getIfNotNull(network.getAzure(), p -> EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder
                        .anEnvironmentNetworkAzureParams()
                        .withNetworkId(p.getNetworkId())
                        .withResourceGroupName(p.getResourceGroupName())
                        .withNoPublicIp(p.isNoPublicIp())
                        .build()))
                .withYarn(getIfNotNull(network.getYarn(), p -> EnvironmentNetworkYarnParams.EnvironmentNetworkYarnParamsBuilder
                        .anEnvironmentNetworkYarnParams()
                        .withQueue(p.getQueue())
                        .build()))
                .withMock(getIfNotNull(network.getMock(), p -> EnvironmentNetworkMockParams.EnvironmentNetworkMockParamsBuilder
                        .anEnvironmentNetworkMockParams()
                        .withVpcId(p.getVpcId())
                        .withInternetGatewayId(p.getInternetGatewayId())
                        .build()))
                .build();
    }
}
