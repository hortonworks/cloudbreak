package com.sequenceiq.environment.environment.converter.network;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.api.environment.model.EnvironmentNetworkAzureV1Params;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentNetworkV1Request;
import com.sequenceiq.environment.api.environment.model.response.EnvironmentNetworkV1Response;
import com.sequenceiq.environment.environment.domain.network.AzureNetwork;
import com.sequenceiq.environment.environment.domain.network.BaseNetwork;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV1Request source) {
        EnvironmentNetworkAzureV1Params azureParams = source.getAzure();
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(azureParams.getNetworkId());
        azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
        azureNetwork.setNoPublicIp(azureParams.getNoPublicIp());
        azureNetwork.setNoFirewallRules(azureParams.getNoFirewallRules());
        return azureNetwork;
    }

    @Override
    EnvironmentNetworkV1Response setProviderSpecificFields(EnvironmentNetworkV1Response result, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        EnvironmentNetworkAzureV1Params azureV1Params = new EnvironmentNetworkAzureV1Params();
        azureV1Params.setNetworkId(azureNetwork.getNetworkId());
        azureV1Params.setResourceGroupName(azureNetwork.getResourceGroupName());
        azureV1Params.setNoPublicIp(azureNetwork.getNoPublicIp());
        azureV1Params.setNoFirewallRules(azureNetwork.getNoFirewallRules());
        result.setAzure(azureV1Params);
        return result;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
