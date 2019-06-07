package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.domain.AzureNetwork;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAzureNetworkProvider implements FreeIpaNetworkProvider {
    @Override
    public NetworkRequest provider(Environment environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        AzureNetwork network = (AzureNetwork) environment.getNetwork();
        AzureNetworkParameters azureNetworkParameters = new AzureNetworkParameters();
        azureNetworkParameters.setNetworkId(network.getNetworkId());
        azureNetworkParameters.setNoFirewallRules(network.getNoFirewallRules());
        azureNetworkParameters.setNoPublicIp(network.getNoFirewallRules());
        azureNetworkParameters.setResourceGroupName(network.getResourceGroupName());
        azureNetworkParameters.setSubnetId(network.getSubnetIdsSet().iterator().next());
        networkRequest.setAzure(azureNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, Environment environment) {
        AzureNetworkParameters azureNetwork = networkRequest.getAzure();
        return environment.getNetwork().getSubnetMetasMap().get(azureNetwork.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
