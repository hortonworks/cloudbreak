package com.sequenceiq.environment.network;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Service
public class CloudNetworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetworkService.class);

    private final PlatformParameterService platformParameterService;

    public CloudNetworkService(PlatformParameterService platformParameterService) {
        this.platformParameterService = platformParameterService;
    }

    public Map<String, CloudSubnet> retrieveSubnetMetadata(EnvironmentDto environmentDto, NetworkDto network) {
        LOGGER.debug("retrieveSubnetMetadata() has called with the following parameters: EnvironmentDto -> {}, NetworkDto -> {}", environmentDto, network);
        if (network == null || network.getSubnetIds().isEmpty()) {
            return Map.of();
        } else if (isAws(environmentDto.getCloudPlatform())) {
            Map<String, String> filter = getAwsVpcId(network)
                    .map(vpcId -> Map.of("vpcId", vpcId))
                    .orElse(Map.of());
            return fetchCloudNetwork(environmentDto.getRegions(), environmentDto.getCredential(), environmentDto.getCloudPlatform(), network, filter);
        } else if (isAzure(environmentDto.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put("networkId", network.getAzure().getNetworkId());
            filter.put("resourceGroupName", network.getAzure().getResourceGroupName());
            return fetchCloudNetwork(environmentDto.getRegions(), environmentDto.getCredential(), environmentDto.getCloudPlatform(), network, filter);
        } else {
            return network.getSubnetIds().stream().collect(toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    public Map<String, CloudSubnet> retrieveSubnetMetadata(Environment environment, NetworkDto network) {
        LOGGER.debug("retrieveSubnetMetadata() has called with the following parameters: Environment -> {}, NetworkDto -> {}", environment, network);
        if (network == null || network.getSubnetIds().isEmpty()) {
            return Map.of();
        } else if (isAws(environment.getCloudPlatform())) {
            Map<String, String> filter = getAwsVpcId(network)
                    .map(vpcId -> Map.of("vpcId", vpcId))
                    .orElse(Map.of());
            return fetchCloudNetwork(environment.getRegionSet(), environment.getCredential(), environment.getCloudPlatform(), network, filter);
        } else if (isAzure(environment.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put("networkId", network.getAzure().getNetworkId());
            filter.put("resourceGroupName", network.getAzure().getResourceGroupName());
            return fetchCloudNetwork(environment.getRegionSet(), environment.getCredential(), environment.getCloudPlatform(), network, filter);
        } else {
            return network.getSubnetIds().stream().collect(toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    private Optional<String> getAwsVpcId(NetworkDto networkDto) {
        return Optional.ofNullable(networkDto)
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId);
    }

    private Map<String, CloudSubnet> fetchCloudNetwork(Set<Region> regions, Credential credential, String cloudPlatform, NetworkDto network,
            Map<String, String> filter) {
        String regionName = regions.iterator().next().getName();
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(credential);
        platformResourceRequest.setCloudPlatform(cloudPlatform);
        platformResourceRequest.setRegion(regionName);
        platformResourceRequest.setFilters(filter);

        LOGGER.debug("About to fetch networks from cloud provider ({})...", cloudPlatform);
        CloudNetworks cloudNetworks = platformParameterService.getCloudNetworks(platformResourceRequest);
        Set<CloudNetwork> cloudNetworkSet = cloudNetworks.getCloudNetworkResponses().get(regionName);
        return cloudNetworkSet.stream()
                .flatMap(it -> it.getSubnetsMeta().stream())
                .filter(sn -> network.getSubnetIds().contains(sn.getId()))
                .collect(toMap(CloudSubnet::getId, Function.identity()));
    }

    private boolean isAzure(String cloudPlatform) {
        return CloudPlatform.AZURE.name().equalsIgnoreCase(cloudPlatform);
    }

    private boolean isAws(String cloudPlatform) {
        return CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform);
    }

}
