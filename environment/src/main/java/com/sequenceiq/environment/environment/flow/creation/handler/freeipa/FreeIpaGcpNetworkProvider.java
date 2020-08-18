package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.GcpParams;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.GcpNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;

@Component
public class FreeIpaGcpNetworkProvider implements FreeIpaNetworkProvider {

    @Override
    public NetworkRequest provider(EnvironmentDto environment) {
        NetworkRequest networkRequest = new NetworkRequest();
        GcpParams gcpParams = environment.getNetwork().getGcp();
        GcpNetworkParameters gcpNetworkParameters = new GcpNetworkParameters();
        gcpNetworkParameters.setNetworkId(gcpParams.getNetworkId());
        gcpNetworkParameters.setNoFirewallRules(gcpParams.getNoFirewallRules());
        gcpNetworkParameters.setNoPublicIp(gcpParams.getNoPublicIp());
        gcpNetworkParameters.setSharedProjectId(gcpParams.getSharedProjectId());
        gcpNetworkParameters.setSubnetId(environment.getNetwork().getSubnetIds().iterator().next());
        networkRequest.setGcp(gcpNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        GcpNetworkParameters gcpNetworkParameters = networkRequest.getGcp();
        return environment.getNetwork().getSubnetMetas().get(gcpNetworkParameters.getSubnetId()).getAvailabilityZone();
    }

    @Override
    public Set<String> getSubnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getGcp().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.GCP;
    }
}
