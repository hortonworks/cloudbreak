package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaAwsNetworkProvider implements FreeIpaNetworkProvider {

    @Inject
    private SubnetIdProvider subnetIdProvider;

    @Override
    public NetworkRequest provider(EnvironmentDto environment) {
        NetworkDto network = environment.getNetwork();
        NetworkRequest networkRequest = new NetworkRequest();
        AwsParams awsParams = network.getAws();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        networkRequest.setNetworkCidrs(collectNetworkCidrs(network));
        networkRequest.setOutboundInternetTraffic(network.getOutboundInternetTraffic());
        awsNetworkParameters.setVpcId(awsParams.getVpcId());
        awsNetworkParameters.setSubnetId(
                subnetIdProvider.provide(environment.getNetwork(), environment.getExperimentalFeatures().getTunnel(), CloudPlatform.AWS));
        networkRequest.setAws(awsNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        AwsNetworkParameters awsNetwork = networkRequest.getAws();
        return environment.getNetwork().getSubnetMetas().get(awsNetwork.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public Set<String> getSubnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getAws().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    private List<String> collectNetworkCidrs(NetworkDto network) {
        return CollectionUtils.isNotEmpty(network.getNetworkCidrs()) ? new ArrayList<>(network.getNetworkCidrs()) : List.of();
    }
}
