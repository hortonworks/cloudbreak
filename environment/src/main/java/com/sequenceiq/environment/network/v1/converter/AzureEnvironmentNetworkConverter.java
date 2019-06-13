package com.sequenceiq.environment.network.v1.converter;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(NetworkDto network) {
        AzureParams azureParams = network.getAzure();

        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(azureParams.getNetworkId());
        azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
        azureNetwork.setNoPublicIp(azureParams.isNoPublicIp());
        azureNetwork.setNoFirewallRules(azureParams.isNoFirewallRules());
        return azureNetwork;
    }

    @Override
    public BaseNetwork setProviderSpecificNetwork(BaseNetwork baseNetwork, CreatedCloudNetwork createdCloudNetwork) {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        azureNetwork.setNetworkId(createdCloudNetwork.getNetworkId());
        azureNetwork.setResourceGroupName(String.valueOf(createdCloudNetwork.getProperties().get("resourceGroupName")));
        azureNetwork.setSubnetIds(createdCloudNetwork.getSubnets().stream().map(CreatedSubnet::getSubnetId).collect(Collectors.toSet()));
        azureNetwork.setSubnetMetas(createdCloudNetwork.getSubnets().stream()
                .collect(Collectors.toMap(
                        CreatedSubnet::getSubnetId,
                        subnet -> new CloudSubnet(
                                subnet.getSubnetId(),
                                subnet.getSubnetId(),
                                subnet.getAvailabilityZone()))));
        return azureNetwork;
    }

    @Override
    EnvironmentNetworkResponse setProviderSpecificFields(EnvironmentNetworkResponse result, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        EnvironmentNetworkAzureParams azureV1Params = new EnvironmentNetworkAzureParams();
        azureV1Params.setNetworkId(azureNetwork.getNetworkId());
        azureV1Params.setResourceGroupName(azureNetwork.getResourceGroupName());
        azureV1Params.setNoPublicIp(azureNetwork.getNoPublicIp());
        azureV1Params.setNoFirewallRules(azureNetwork.getNoFirewallRules());
        result.setAzure(azureV1Params);
        return result;
    }

    @Override
    NetworkDto setProviderSpecificFields(NetworkDto.Builder builder, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        return builder.withAzure(
                AzureParams.AzureParamsBuilder.anAzureParams()
                        .withNetworkId(azureNetwork.getNetworkId())
                        .withResourceGroupName(azureNetwork.getResourceGroupName())
                        .withNoFirewallRules(azureNetwork.getNoFirewallRules())
                        .withNoPublicIp(azureNetwork.getNoPublicIp())
                        .build())
                .build();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public boolean hasExistingNetwork(BaseNetwork baseNetwork) {
        return Optional.ofNullable((AzureNetwork) baseNetwork).map(AzureNetwork::getNetworkId).isPresent();
    }
}
