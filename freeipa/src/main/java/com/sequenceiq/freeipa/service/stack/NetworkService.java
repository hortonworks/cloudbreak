package com.sequenceiq.freeipa.service.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
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
        CloudNetworks cloudNetworks = fetchCloudNetworks(environmentCrn, stack, networkId, subnetIds);
        ArrayListMultimap<String, String> filteredSubnetsWithCidr = filterNetworkResponse(stack, networkId, subnetIds, cloudNetworks);
        LOGGER.debug("Filtering result: {}", filteredSubnetsWithCidr);
        return filteredSubnetsWithCidr;
    }

    private ArrayListMultimap<String, String> filterNetworkResponse(Stack stack, String networkId, Collection<String> subnetIds, CloudNetworks cloudNetworks) {
        return cloudNetworks.getCloudNetworkResponses()
                .getOrDefault(stack.getRegion(), Collections.emptySet()).stream()
                .filter(cloudNetwork -> filterNetwork(networkId, cloudNetwork))
                .peek(cloudNetwork -> LOGGER.debug("CloudNetwork passed filtering name: [{}] id: [{}]", cloudNetwork.getName(), cloudNetwork.getId()))
                .flatMap(cloudNetwork -> cloudNetwork.getSubnetsMeta().stream())
                .filter(cloudSubnet -> StringUtils.isNoneBlank(cloudSubnet.getId(), cloudSubnet.getCidr()))
                .filter(cloudSubnet -> subnetIds.contains(cloudSubnet.getId()) || subnetIds.contains(cloudSubnet.getName()))
                .collect(Multimaps.toMultimap(CloudSubnet::getId, CloudSubnet::getCidr, ArrayListMultimap::create));
    }

    private CloudNetworks fetchCloudNetworks(String environmentCrn, Stack stack, String networkId, Collection<String> subnetIds) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        Map<String, String> filter = getCloudNetworkFilter(stack, networkId, subnetIds);
        CloudNetworks cloudNetworks = cloudParameterService.getCloudNetworks(cloudCredential, stack.getRegion(), stack.getPlatformvariant(), filter);
        LOGGER.debug("Received Cloud networks for region [{}]: {}", stack.getRegion(), cloudNetworks.getCloudNetworkResponses().get(stack.getRegion()));
        return cloudNetworks;
    }

    private boolean filterNetwork(String networkId, com.sequenceiq.cloudbreak.cloud.model.CloudNetwork cloudNetwork) {
        String cloudNetworkId = transformAzureNetworkId(cloudNetwork);
        return networkId.equals(cloudNetworkId) || networkId.equals(cloudNetwork.getName());
    }

    private String transformAzureNetworkId(com.sequenceiq.cloudbreak.cloud.model.CloudNetwork cloudNetwork) {
        String[] splittedNetworkId = cloudNetwork.getId().split("/");
        return splittedNetworkId[splittedNetworkId.length - 1];
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
