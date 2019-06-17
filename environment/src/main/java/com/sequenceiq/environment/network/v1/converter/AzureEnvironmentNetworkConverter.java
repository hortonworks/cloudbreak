package com.sequenceiq.environment.network.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.network.domain.AzureNetwork;
import com.sequenceiq.environment.network.domain.BaseNetwork;
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
}
