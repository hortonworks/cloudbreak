package com.sequenceiq.freeipa.service.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.filter.NetworkFilterProvider;

@Service
public class NetworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialService credentialService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private Map<CloudPlatform, NetworkFilterProvider> networkFilterProviderMap;

    public Multimap<String, String> getFilteredSubnetWithCidr(Stack stack) {
        Map<String, Object> attributes = stack.getNetwork().getAttributes().getMap();
        String networkId = (String) attributes.getOrDefault("networkId", attributes.get("vpcId"));
        Objects.requireNonNull(networkId, "Network id is null");
        return getFilteredSubnetWithCidr(stack.getEnvironmentCrn(), stack, networkId, collectSubnetIdsFromStack(stack));
    }

    public Multimap<String, String> getFilteredSubnetWithCidr(String environmentCrn, Stack stack, String networkId, Collection<String> subnetIds) {
        LOGGER.debug("NetworkId: [{}] SubnetIds: {}", networkId, subnetIds);
        Set<CloudNetwork> cloudNetworks = fetchCloudNetworks(environmentCrn, stack, networkId, subnetIds);
        StringBuilder filterLogger = new StringBuilder();
        ArrayListMultimap<String, String> filteredSubnetsWithCidr = filterNetworkResponse(networkId, subnetIds, cloudNetworks, filterLogger);
        LOGGER.debug("Filtering result: {} Filtering debug info: {}", filteredSubnetsWithCidr, filterLogger);
        return filteredSubnetsWithCidr;
    }

    private ArrayListMultimap<String, String> filterNetworkResponse(String networkId, Collection<String> subnetIds, Set<CloudNetwork> cloudNetworks,
            StringBuilder filterLogger) {
        return cloudNetworks.stream()
                .filter(cloudNetwork -> filterNetwork(networkId, cloudNetwork, filterLogger))
                .peek(cloudNetwork -> filterLogger.append(System.lineSeparator()).append(String.format("CloudNetwork passed filtering name: [%s] id: [%s].",
                        cloudNetwork.getName(), cloudNetwork.getId())))
                .flatMap(cloudNetwork -> cloudNetwork.getSubnetsMeta().stream())
                .filter(cloudSubnet -> StringUtils.isNoneBlank(cloudSubnet.getId(), cloudSubnet.getCidr()))
                .filter(cloudSubnet -> subnetIds.contains(cloudSubnet.getId()) || subnetIds.contains(cloudSubnet.getName()))
                .collect(Multimaps.toMultimap(CloudSubnet::getId, CloudSubnet::getCidr, ArrayListMultimap::create));
    }

    private Set<CloudNetwork> fetchCloudNetworks(String environmentCrn, Stack stack, String networkId, Collection<String> subnetIds) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        Map<String, String> filter = getCloudNetworkFilter(stack, networkId, subnetIds);
        Set<CloudNetwork> cloudNetworks = cloudParameterService.getCloudNetworks(cloudCredential, stack.getRegion(), stack.getPlatformvariant(), filter)
                .getCloudNetworkResponses().getOrDefault(stack.getRegion(), Set.of());
        LOGGER.debug("Received Cloud networks for region [{}]: {}", stack.getRegion(), cloudNetworks);
        return cloudNetworks;
    }

    private boolean filterNetwork(String networkId, com.sequenceiq.cloudbreak.cloud.model.CloudNetwork cloudNetwork, StringBuilder filterLogger) {
        String cloudNetworkId = transformAzureNetworkId(cloudNetwork, filterLogger);
        boolean equalsResult = networkId.equals(cloudNetworkId) || networkId.equals(cloudNetwork.getName());
        filterLogger.append(System.lineSeparator())
                .append(String.format("Result is [%s] for comparing network ID [%s] with cloud network ID [%s] and cloud network name [%s].",
                equalsResult, networkId, cloudNetworkId, cloudNetwork.getName()));
        return equalsResult;
    }

    private String transformAzureNetworkId(com.sequenceiq.cloudbreak.cloud.model.CloudNetwork cloudNetwork, StringBuilder filterLogger) {
        String[] splittedNetworkId = cloudNetwork.getId().split("/");
        String transformedNetworkId = splittedNetworkId[splittedNetworkId.length - 1];
        if (splittedNetworkId.length > 1) {
            filterLogger.append(System.lineSeparator())
                    .append(String.format("Transformed [%s] network ID to [%s].", cloudNetwork.getId(), transformedNetworkId));
        }
        return transformedNetworkId;
    }

    private Map<String, String> getCloudNetworkFilter(Stack stack, String networkId, Collection<String> subnetIds) {
        NetworkFilterProvider networkFilterProvider = networkFilterProviderMap.get(CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (networkFilterProvider == null) {
            LOGGER.debug("networkFilterProvider is null for [{}]", stack.getCloudPlatform());
            return Map.of();
        } else {
            LOGGER.debug("NetworkFilterProvider found for [{}]", stack.getCloudPlatform());
            Map<String, String> networkFilter = networkFilterProvider.provide(stack.getNetwork(), networkId, subnetIds);
            LOGGER.debug("NetworkFilter: {}", networkFilter);
            return networkFilter;
        }
    }

    private Set<String> collectSubnetIdsFromStack(Stack stack) {
        Set<String> subnetIds = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
            if (instanceGroupNetwork != null && instanceGroupNetwork.getAttributes() != null) {
                Map<String, Object> map = instanceGroupNetwork.getAttributes().getMap();
                subnetIds.addAll((List<String>) map.getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>()));
            }
        }
        return subnetIds;
    }
}
