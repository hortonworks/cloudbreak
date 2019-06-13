package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAzureNetworkProvider implements FreeIpaNetworkProvider {
    @Override
    public NetworkRequest provider(EnvironmentDto environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        AzureParams azureParams = environment.getNetwork().getAzure();
        AzureNetworkParameters azureNetworkParameters = new AzureNetworkParameters();
        azureNetworkParameters.setNetworkId(azureParams.getNetworkId());
        azureNetworkParameters.setNoFirewallRules(azureParams.isNoFirewallRules());
        azureNetworkParameters.setNoPublicIp(azureParams.isNoPublicIp());
        azureNetworkParameters.setResourceGroupName(azureParams.getResourceGroupName());
        azureNetworkParameters.setSubnetId(environment.getNetwork().getSubnetMetas().keySet().iterator().next());
        networkRequest.setAzure(azureNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        AzureNetworkParameters azureNetwork = networkRequest.getAzure();
        return environment.getNetwork().getSubnetMetas().get(azureNetwork.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
