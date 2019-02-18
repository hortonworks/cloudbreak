package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class NetworkToNetworkV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, NetworkV4Response> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkToNetworkV4ResponseConverter.class);

    private static final List<ResourceType> NETWORK_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AZURE_NETWORK,
            ResourceType.GCP_NETWORK,
            ResourceType.OPENSTACK_NETWORK);

    private static final List<ResourceType> SUBNET_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AWS_SUBNET,
            ResourceType.AZURE_SUBNET,
            ResourceType.GCP_SUBNET,
            ResourceType.OPENSTACK_SUBNET);

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public NetworkV4Response convert(Stack source) {
        NetworkV4Response networkResp = null;
        Network network = source.getNetwork();
        if (network != null) {
            networkResp = new NetworkV4Response();
            networkResp.setSubnetCIDR(network.getSubnetCIDR());
            if (network.getAttributes() != null) {
                Map<String, Object> parameters = cleanMap(network.getAttributes().getMap());
                putNetworkResourcesIntoResponse(source, parameters);
                providerParameterCalculator.parse(parameters, networkResp);
            }
        }
        return networkResp;
    }

    private void putNetworkResourcesIntoResponse(Stack stack, Map<String, Object> parameters) {
        Set<Resource> resources = stack.getResources();

        for (Resource aResource : resources) {
            if (ResourceType.AWS_VPC.equals(aResource.getResourceType())) {
                parameters.put("vpcId", aResource.getResourceName());
            } else if (anyMatchOfResourceTypeIn(aResource, NETWORK_RESOURCE_TYPES)) {
                parameters.put("networkId", aResource.getResourceName());
            } else if (anyMatchOfResourceTypeIn(aResource, SUBNET_RESOURCE_TYPES)) {
                parameters.put("subnetId", aResource.getResourceName());
            }
        }
    }

    private boolean anyMatchOfResourceTypeIn(Resource aResource, List<ResourceType> identifierResources) {
        return identifierResources.stream().anyMatch(subnetIdentifier -> aResource.getResourceType().equals(subnetIdentifier));
    }
}
