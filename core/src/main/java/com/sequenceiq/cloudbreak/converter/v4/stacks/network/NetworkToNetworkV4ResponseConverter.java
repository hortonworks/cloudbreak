package com.sequenceiq.cloudbreak.converter.v4.stacks.network;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.MapUtil.cleanMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
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

    @Inject
    private ResourceService resourceService;

    @Nullable
    public NetworkV4Response convert(StackDtoDelegate source) {
        if (source == null) {
            return null;
        }
        LOGGER.debug("Converting {} to {} from the content of: {}", source.getClass().getSimpleName(), NetworkV4Response.class.getSimpleName(), source);
        NetworkV4Response networkResp = null;
        Optional<Network> network = source.getNetwork() != null ? ofNullable(source.getNetwork()) : empty();
        if (network.isPresent()) {
            networkResp = new NetworkV4Response();
            networkResp.setSubnetCIDR(network.get().getSubnetCIDR());
            if (network.get().getAttributes() != null) {
                Map<String, Object> parameters = cleanMap(network.get().getAttributes().getMap());
                putNetworkResourcesIntoResponse(source.getId(), parameters);
                providerParameterCalculator.parse(parameters, networkResp);
            }
        }
        LOGGER.debug("Conversion from {} to {} is done with the result of: {}",
                Stack.class.getSimpleName(), NetworkV4Response.class.getSimpleName(), networkResp);
        return networkResp;
    }

    private void putNetworkResourcesIntoResponse(Long stackId, Map<String, Object> parameters) {
        List<ResourceType> types = new ArrayList<>();
        types.add(ResourceType.AWS_VPC);
        types.addAll(NETWORK_RESOURCE_TYPES);
        types.addAll(SUBNET_RESOURCE_TYPES);
        List<Resource> resources = resourceService.findAllByStackIdAndResourceTypeIn(stackId, types);
        for (Resource aResource : resources) {
            if (ResourceType.AWS_VPC.equals(aResource.getResourceType())) {
                parameters.put("vpcId", aResource.getResourceName());
            } else if (anyMatchOfResourceTypeIn(aResource, NETWORK_RESOURCE_TYPES)) {
                parameters.put("networkId", aResource.getResourceName());
            } else if (anyMatchOfResourceTypeIn(aResource, SUBNET_RESOURCE_TYPES)) {
                parameters.put(SUBNET_ID, aResource.getResourceName());
            }
        }
    }

    private boolean anyMatchOfResourceTypeIn(Resource aResource, List<ResourceType> identifierResources) {
        return identifierResources.stream().anyMatch(subnetIdentifier -> aResource.getResourceType().equals(subnetIdentifier));
    }

}
