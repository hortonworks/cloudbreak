package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;

@Component
public class NetworkDtoToResponseConverter {

    private final SubnetIdProvider subnetIdProvider;

    public NetworkDtoToResponseConverter(SubnetIdProvider subnetIdProvider) {
        this.subnetIdProvider = subnetIdProvider;
    }

    public EnvironmentNetworkResponse convert(NetworkDto network, Tunnel tunnel, boolean detailedResponse) {
        ProvidedSubnetIds preferedSubnetId = getPreferedSubnetId(network, tunnel, detailedResponse);

        return EnvironmentNetworkResponse.builder()
                .withCrn(network.getResourceCrn())
                .withSubnetIds(network.getSubnetIds())
                .withNetworkCidr(network.getNetworkCidr())
                .withNetworkCidrs(network.getNetworkCidrs())
                .withSubnetMetas(network.getSubnetMetas())
                .withCbSubnets(network.getCbSubnets())
                .withDwxSubnets(network.getDwxSubnets())
                .withMlxSubnets(network.getMlxSubnets())
                .withLiftieSubnets(network.getMlxSubnets())
                .withPreferedSubnetId(getPreferedSubnetId(preferedSubnetId, detailedResponse))
                .withPreferedSubnetIds(getPreferedSubnetIds(preferedSubnetId, detailedResponse))
                .withPrivateSubnetCreation(network.getPrivateSubnetCreation())
                .withServiceEndpointCreation(network.getServiceEndpointCreation())
                .withOutboundInternetTraffic(network.getOutboundInternetTraffic())
                .withExistingNetwork(RegistrationType.EXISTING == network.getRegistrationType())
                .withUsePublicEndpointAccessGateway(network.getPublicEndpointAccessGateway())
                .withEndpointGatewaySubnetMetas(network.getEndpointGatewaySubnetMetas())
                .withEndpointGatewaySubnetIds(network.getEndpointGatewaySubnetIds())
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
                .withGcp(getIfNotNull(network.getGcp(), p -> EnvironmentNetworkGcpParams.EnvironmentNetworkGcpParamsBuilder
                        .anEnvironmentNetworkGcpParamsBuilder()
                        .withNetworkId(p.getNetworkId())
                        .withSharedProjectId(p.getSharedProjectId())
                        .withNoFirewallRules(p.getNoFirewallRules())
                        .withNoPublicIp(p.getNoPublicIp())
                        .build()))
                .withYarn(getIfNotNull(network.getYarn(), p -> EnvironmentNetworkYarnParams.EnvironmentNetworkYarnParamsBuilder
                        .anEnvironmentNetworkYarnParams()
                        .withQueue(p.getQueue())
                        .withLifetime(p.getLifetime())
                        .build()))
                .withMock(getIfNotNull(network.getMock(), p -> EnvironmentNetworkMockParams.EnvironmentNetworkMockParamsBuilder
                        .anEnvironmentNetworkMockParams()
                        .withVpcId(p.getVpcId())
                        .withInternetGatewayId(p.getInternetGatewayId())
                        .build()))
                .build();
    }

    public ProvidedSubnetIds getPreferedSubnetId(NetworkDto network, Tunnel tunnel, boolean detailedResponse) {
        ProvidedSubnetIds providedSubnetIds = null;
        if (detailedResponse) {
            providedSubnetIds = subnetIdProvider.subnets(
                    network,
                    tunnel,
                    network.getCloudPlatform(),
                    false);
        }
        return providedSubnetIds;
    }

    public String getPreferedSubnetId(ProvidedSubnetIds providedSubnetIds, boolean detailedResponse) {
        String subnetId = null;
        if (detailedResponse) {
            if (providedSubnetIds != null) {
                subnetId = providedSubnetIds.getSubnetId();
            }
        }
        return subnetId;
    }

    public Set<String> getPreferedSubnetIds(ProvidedSubnetIds providedSubnetIds, boolean detailedResponse) {
        Set<String> subnetIds = new HashSet<>();
        if (detailedResponse) {
            if (providedSubnetIds != null) {
                subnetIds = providedSubnetIds.getSubnetIds();
            }
        }
        return subnetIds;
    }
}
