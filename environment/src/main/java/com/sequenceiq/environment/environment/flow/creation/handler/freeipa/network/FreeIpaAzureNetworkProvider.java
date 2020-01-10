package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.network;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAzureNetworkProvider implements FreeIpaNetworkProvider {

    @Inject
    private SubnetIdProvider subnetIdProvider;

    @Override
    public NetworkRequest provider(EnvironmentDto environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        AzureParams azureParams = environment.getNetwork().getAzure();
        AzureNetworkParameters azureNetworkParameters = new AzureNetworkParameters();
        azureNetworkParameters.setNetworkId(azureParams.getNetworkId());
        azureNetworkParameters.setNoPublicIp(azureParams.isNoPublicIp());
        azureNetworkParameters.setResourceGroupName(azureParams.getResourceGroupName());
        azureNetworkParameters.setSubnetId(subnetIdProvider.provide(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel()));
        networkRequest.setAzure(azureNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        return null;
    }

    @Override
    public Set<String> getSubnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getAzure().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
