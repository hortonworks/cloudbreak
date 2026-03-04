package com.sequenceiq.environment.environment.flow.creation.handler.freeipa.openstack;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaNetworkProvider;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.OpenStackNetworkParameters;

@Component
public class FreeIpaOpenstackNetworkProvider implements FreeIpaNetworkProvider {
    @Override
    public NetworkRequest network(EnvironmentDto environment, boolean multiAzRequired) {
        if (multiAzRequired) {
            throw new UnsupportedOperationException("Multi-AZ is not supported for OpenStack");
        }
        NetworkDto network = environment.getNetwork();
        OpenStackNetworkParameters openStackNetworkParameters = new OpenStackNetworkParameters();
        openStackNetworkParameters.setNetworkId(network.getNetworkId());
        // TODO: Openstack - one subnet supported for now
        if (network.getSubnetIds() != null) {
            openStackNetworkParameters.setSubnetId(network.getSubnetIds().iterator().next());
        }
        openStackNetworkParameters.setRouterId(network.getOpenstack().getRouterId());
        openStackNetworkParameters.setPublicNetId(network.getOpenstack().getPublicNetId());
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setOpenstack(openStackNetworkParameters);
        return networkRequest;
    }

    @Override
    public String availabilityZone(NetworkRequest networkRequest, EnvironmentDto environment) {
        return null;
    }

    @Override
    public Set<String> subnets(NetworkRequest networkRequest) {
        return Sets.newHashSet(networkRequest.getOpenstack().getSubnetId());
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.OPENSTACK;
    }
}
