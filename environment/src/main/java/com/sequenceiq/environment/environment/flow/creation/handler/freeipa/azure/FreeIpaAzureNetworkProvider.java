package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.azure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaNetworkProvider;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AzureNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAzureNetworkProvider implements FreeIpaNetworkProvider {

    @Inject
    private SubnetIdProvider subnetIdProvider;

    @Override
    public NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired) {
        NetworkRequest networkRequest = new NetworkRequest();
        NetworkDto network = environment.getNetwork();
        AzureParams azureParams = network.getAzure();
        AzureNetworkParameters azureNetworkParameters = new AzureNetworkParameters();
        azureNetworkParameters.setNetworkId(azureParams.getNetworkId());
        azureNetworkParameters.setNoPublicIp(azureParams.isNoPublicIp());
        azureNetworkParameters.setResourceGroupName(azureParams.getResourceGroupName());
        ProvidedSubnetIds providedSubnetIds = subnetIdProvider.subnets(
                network,
                environment.getExperimentalFeatures().getTunnel(),
                CloudPlatform.AZURE,
                multiAzRequired);
        azureNetworkParameters.setSubnetId(providedSubnetIds.getSubnetId());
        networkRequest.setAzure(azureNetworkParameters);
        networkRequest.setNetworkCidrs(collectNetworkCidrs(network));
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        return null;
    }

    @Override
    public Set<String> subnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getAzure().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }

    private List<String> collectNetworkCidrs(NetworkDto network) {
        return CollectionUtils.isNotEmpty(network.getNetworkCidrs()) ? new ArrayList<>(network.getNetworkCidrs()) : List.of();
    }
}
