package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.gcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaNetworkProvider;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.GcpNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaGcpNetworkProvider implements FreeIpaNetworkProvider {

    @Override
    public NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired) {
        NetworkRequest networkRequest = new NetworkRequest();
        NetworkDto network = environment.getNetwork();
        GcpParams gcpParams = network.getGcp();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setNetworkId(gcpParams.getNetworkId());
        gcpNetworkParameters.setNoFirewallRules(gcpParams.getNoFirewallRules());
        gcpNetworkParameters.setNoPublicIp(gcpParams.getNoPublicIp());
        gcpNetworkParameters.setSharedProjectId(gcpParams.getSharedProjectId());
        gcpNetworkParameters.setSubnetId(network.getSubnetIds().iterator().next());
        networkRequest.setGcp(gcpNetworkParameters);
        networkRequest.setNetworkCidrs(collectNetworkCidrs(network));
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        GcpNetworkParameters gcpNetworkParameters = networkRequest.getGcp();
        return environment.getNetwork().getSubnetMetas().get(gcpNetworkParameters.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public Set<String> subnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getGcp().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }

    private List<String> collectNetworkCidrs(NetworkDto network) {
        return CollectionUtils.isNotEmpty(network.getNetworkCidrs()) ? new ArrayList<>(network.getNetworkCidrs()) : List.of();
    }
}
