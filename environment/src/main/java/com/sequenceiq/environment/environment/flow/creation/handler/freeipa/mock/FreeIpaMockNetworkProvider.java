package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.mock;

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
import com.sequenceiq.environment.network.dto.MockParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.MockNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaMockNetworkProvider implements FreeIpaNetworkProvider {

    @Inject
    private SubnetIdProvider subnetIdProvider;

    @Override
    public NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired) {
        NetworkDto network = environment.getNetwork();
        NetworkRequest networkRequest = new NetworkRequest();
        MockParams mockParam = network.getMock();
        MockNetworkParameters mockNetworkParameters = new MockNetworkParameters();
        networkRequest.setNetworkCidrs(collectNetworkCidrs(network));
        networkRequest.setOutboundInternetTraffic(network.getOutboundInternetTraffic());
        mockNetworkParameters.setVpcId(mockParam.getVpcId());
        if (!multiAzRequired) {
            ProvidedSubnetIds providedSubnetIds = subnetIdProvider.subnets(
                    environment.getNetwork(),
                    environment.getExperimentalFeatures().getTunnel(),
                    CloudPlatform.MOCK,
                    multiAzRequired);
            mockNetworkParameters.setSubnetId(providedSubnetIds.getSubnetId());
        }
        networkRequest.setMock(mockNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        MockNetworkParameters mockNetwork = networkRequest.getMock();
        if (mockNetwork.getSubnetId() != null) {
            return environment.getNetwork().getSubnetMetas().get(mockNetwork.getSubnetId()).getAvailabilityZone();
        }
        return null;
    }

    @Override
    public Set<String> subnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getMock().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.MOCK;
    }

    private List<String> collectNetworkCidrs(NetworkDto network) {
        return CollectionUtils.isNotEmpty(network.getNetworkCidrs()) ? new ArrayList<>(network.getNetworkCidrs()) : List.of();
    }
}
