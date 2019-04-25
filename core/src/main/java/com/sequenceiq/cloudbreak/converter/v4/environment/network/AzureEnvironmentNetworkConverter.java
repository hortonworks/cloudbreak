package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.EnvironmentNetworkV4Response;
import com.sequenceiq.cloudbreak.domain.environment.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    BaseNetwork createProviderSpecificNetwork(EnvironmentNetworkV4Request source) {
        EnvironmentNetworkAzureV4Params azureParams = source.getAzure();
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(azureParams.getNetworkId());
        azureNetwork.setResourceGroupName(azureParams.getResourceGroupName());
        azureNetwork.setNoPublicIp(azureParams.getNoPublicIp());
        azureNetwork.setNoFirewallRules(azureParams.getNoFirewallRules());
        return azureNetwork;
    }

    @Override
    EnvironmentNetworkV4Response setProviderSpecificFields(EnvironmentNetworkV4Response result, BaseNetwork network) {
        AzureNetwork azureNetwork = (AzureNetwork) network;
        EnvironmentNetworkAzureV4Params azureV4Params = new EnvironmentNetworkAzureV4Params();
        azureV4Params.setNetworkId(azureNetwork.getNetworkId());
        azureV4Params.setResourceGroupName(azureNetwork.getResourceGroupName());
        azureV4Params.setNoPublicIp(azureNetwork.getNoPublicIp());
        azureV4Params.setNoFirewallRules(azureNetwork.getNoFirewallRules());
        result.setAzure(azureV4Params);
        return result;
    }

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(BaseNetwork source) {
        AzureNetwork azureNetwork = (AzureNetwork) source;
        return Map.of(
                "networkId", azureNetwork.getNetworkId(),
                "resourceGroupName", azureNetwork.getResourceGroupName(),
                "noPublicIp", azureNetwork.getNoPublicIp(),
                "noFirewallRules", azureNetwork.getNoFirewallRules());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
