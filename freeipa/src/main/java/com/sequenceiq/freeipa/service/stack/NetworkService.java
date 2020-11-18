package com.sequenceiq.freeipa.service.stack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
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

    public Map<String, String> getFilteredSubnetWithCidr(Stack stack) {
        Map<String, Object> attributes = stack.getNetwork().getAttributes().getMap();
        String networkId = (String) attributes.getOrDefault("networkId", attributes.get("vpcId"));
        String subnetId = (String) attributes.get("subnetId");
        Objects.requireNonNull(networkId, "Network id is null");
        Objects.requireNonNull(subnetId, "Subnet id is null");
        return getFilteredSubnetWithCidr(stack.getEnvironmentCrn(), stack, networkId, Set.of(subnetId));
    }

    public Map<String, String> getFilteredSubnetWithCidr(String environmentCrn, Stack stack, String networkId, Collection<String> subnetIds) {
        Credential credential = credentialService.getCredentialByEnvCrn(environmentCrn);
        ExtendedCloudCredential cloudCredential = extendedCloudCredentialConverter.convert(credential);
        Map<String, String> filter = getFilter(stack, networkId, subnetIds);
        CloudNetworks cloudNetworks =
                cloudParameterService.getCloudNetworks(cloudCredential, stack.getRegion(), stack.getPlatformvariant(), filter);
        LOGGER.debug("Received Cloud networks for region [{}]: {}", stack.getRegion(), cloudNetworks.getCloudNetworkResponses().get(stack.getRegion()));
        return cloudNetworks.getCloudNetworkResponses().getOrDefault(stack.getRegion(), Collections.emptySet()).stream()
                .filter(cloudNetwork -> {
                    // support for azure
                    String[] splittedNetworkId = cloudNetwork.getId().split("/");
                    String cloudNetworkId = splittedNetworkId[splittedNetworkId.length - 1];
                    return networkId.equals(cloudNetworkId) || networkId.equals(cloudNetwork.getName());
                })
                .flatMap(cloudNetwork -> cloudNetwork.getSubnetsMeta().stream())
                .filter(cloudSubnet -> StringUtils.isNoneBlank(cloudSubnet.getId(), cloudSubnet.getCidr()))
                .filter(cloudSubnet -> subnetIds.contains(cloudSubnet.getId()) || subnetIds.contains(cloudSubnet.getName()))
                .collect(Collectors.toMap(CloudSubnet::getId, CloudSubnet::getCidr));
    }

    public Map<String, String> getFilter(Stack stack, String networkId, Collection<String> subnetIds) {
        Map<String, String> filter = new HashMap<>();
        NetworkFilterProvider networkFilterProvider = networkFilterProviderMap.get(CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (networkFilterProvider != null) {
            filter = networkFilterProvider.provide(stack.getNetwork(), networkId, subnetIds);
        }
        return filter;
    }
}
