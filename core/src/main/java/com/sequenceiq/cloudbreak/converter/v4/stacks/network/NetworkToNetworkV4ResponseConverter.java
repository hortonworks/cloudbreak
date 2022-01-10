package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import static com.sequenceiq.cloudbreak.util.MapUtil.cleanMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class NetworkToNetworkV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkToNetworkV4ResponseConverter.class);

    private static final List<ResourceType> NETWORK_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AZURE_NETWORK,
            ResourceType.GCP_NETWORK);

    private static final List<ResourceType> SUBNET_RESOURCE_TYPES = Arrays.asList(
            ResourceType.AWS_SUBNET,
            ResourceType.AZURE_SUBNET,
            ResourceType.GCP_SUBNET);

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Nullable
    public NetworkV4Response convert(Stack source) {
        LOGGER.debug("Converting {} to {} from the content of: {}", Stack.class.getSimpleName(), NetworkV4Response.class.getSimpleName(), source);
        NetworkV4Response networkResp = null;
        Optional<Network> network = source != null ? ofNullable(source.getNetwork()) : empty();
        if (network.isPresent()) {
            networkResp = new NetworkV4Response();
            networkResp.setSubnetCIDR(network.get().getSubnetCIDR());
            if (network.get().getAttributes() != null) {
                Map<String, Object> parameters = cleanMap(network.get().getAttributes().getMap());
                putNetworkResourcesIntoResponse(source, parameters);
                providerParameterCalculator.parse(parameters, networkResp);
            }
        }
        LOGGER.debug("Conversion from {} to {} is done with the result of: {}",
                Stack.class.getSimpleName(), NetworkV4Response.class.getSimpleName(), networkResp);
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
