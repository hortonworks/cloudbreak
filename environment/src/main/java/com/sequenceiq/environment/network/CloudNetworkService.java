package com.sequenceiq.environment.network;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
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
        return getSubnetMetadata(environmentDto, network, network == null ? Set.of() : network.getSubnetIds());
    }

    public Map<String, CloudSubnet> retrieveEndpointGatewaySubnetMetadata(EnvironmentDto environmentDto, NetworkDto network) {
        return getSubnetMetadata(environmentDto, network, network == null ? Set.of() : network.getEndpointGatewaySubnetIds());
    }

    private Map<String, CloudSubnet> getSubnetMetadata(EnvironmentDto environmentDto, NetworkDto network, Set<String> subnetIds) {
        LOGGER.debug("retrieveSubnetMetadata() has called with the following parameters: EnvironmentDto -> {}, NetworkDto -> {}", environmentDto, network);
        if (network == null || subnetIds.isEmpty()) {
            return Map.of();
        } else if (isAws(environmentDto.getCloudPlatform())) {
            Map<String, String> filter = getAwsVpcId(network)
                    .map(vpcId -> Map.of("vpcId", vpcId))
                    .orElse(Map.of());
            return fetchCloudNetwork(environmentDto.getRegions(), environmentDto.getCredential(), environmentDto.getCloudPlatform(),
                network, filter, subnetIds);
        } else if (isAzure(environmentDto.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put("networkId", network.getAzure().getNetworkId());
            filter.put("resourceGroupName", network.getAzure().getResourceGroupName());
            return fetchCloudNetwork(environmentDto.getRegions(), environmentDto.getCredential(), environmentDto.getCloudPlatform(),
                network, filter, subnetIds);
        } else if (isGcp(environmentDto.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put(GcpStackUtil.NETWORK_ID, network.getGcp().getNetworkId());
            filter.put(GcpStackUtil.SHARED_PROJECT_ID, network.getGcp().getSharedProjectId());
            boolean createFireWallRule = false;
            if (!isNullOrEmpty(environmentDto.getSecurityAccess().getCidr())) {
                createFireWallRule = true;
            }
            filter.put(GcpStackUtil.NO_FIREWALL_RULES, String.valueOf(!createFireWallRule));
            filter.put(GcpStackUtil.NO_PUBLIC_IP, String.valueOf(Boolean.TRUE.equals(network.getGcp().getNoPublicIp())));
            buildSubnetIdFilter(subnetIds, filter);
            return fetchCloudNetwork(environmentDto.getRegions(), environmentDto.getCredential(), environmentDto.getCloudPlatform(),
                network, filter, subnetIds);
        } else {
            return subnetIds.stream().collect(toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    public Map<String, CloudSubnet> retrieveSubnetMetadata(Environment environment, NetworkDto network) {
        return getSubnetMetadata(environment, network, network == null ? Set.of() : network.getSubnetIds());
    }

    public Map<String, CloudSubnet> retrieveEndpointGatewaySubnetMetadata(Environment environment, NetworkDto network) {
        return getSubnetMetadata(environment, network, network == null ? Set.of() : network.getEndpointGatewaySubnetIds());
    }

    private Map<String, CloudSubnet> getSubnetMetadata(Environment environment, NetworkDto network, Set<String> subnetIds) {
        LOGGER.debug("retrieveSubnetMetadata() has called with the following parameters: Environment -> {}, NetworkDto -> {}", environment, network);
        if (network == null || subnetIds.isEmpty()) {
            return Map.of();
        } else if (isAws(environment.getCloudPlatform())) {
            Map<String, String> filter = getAwsVpcId(network)
                    .map(vpcId -> Map.of("vpcId", vpcId))
                    .orElse(Map.of());
            return fetchCloudNetwork(environment.getRegionSet(), environment.getCredential(), environment.getCloudPlatform(),
                network, filter, subnetIds);
        } else if (isAzure(environment.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put("networkId", network.getAzure().getNetworkId());
            filter.put("resourceGroupName", network.getAzure().getResourceGroupName());
            return fetchCloudNetwork(environment.getRegionSet(), environment.getCredential(), environment.getCloudPlatform(),
                network, filter, subnetIds);
        } else if (isGcp(environment.getCloudPlatform())) {
            Map<String, String> filter = new HashMap<>();
            filter.put(GcpStackUtil.NETWORK_ID, network.getGcp().getNetworkId());
            filter.put(GcpStackUtil.SHARED_PROJECT_ID, network.getGcp().getSharedProjectId());
            boolean createFireWallRule = false;
            if (!isNullOrEmpty(environment.getCidr())) {
                createFireWallRule = true;
            }
            filter.put(GcpStackUtil.NO_FIREWALL_RULES, String.valueOf(!createFireWallRule));
            filter.put(GcpStackUtil.NO_PUBLIC_IP, String.valueOf(Boolean.TRUE.equals(network.getGcp().getNoPublicIp())));
            buildSubnetIdFilter(subnetIds, filter);
            return fetchCloudNetwork(environment.getRegionSet(), environment.getCredential(), environment.getCloudPlatform(),
                network, filter, subnetIds);
        } else {
            return subnetIds.stream().collect(toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    private void buildSubnetIdFilter(Set<String> subnetIds, Map<String, String> filter) {
        if (!subnetIds.isEmpty()) {
            filter.put("subnetIds", String.join(",", subnetIds));
        }
    }

    private Optional<String> getAwsVpcId(NetworkDto networkDto) {
        return Optional.ofNullable(networkDto)
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId);
    }

    private Map<String, CloudSubnet> fetchCloudNetwork(Set<Region> regions, Credential credential, String cloudPlatform, NetworkDto network,
            Map<String, String> filter, Set<String> subnetIds) {
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
                .filter(sn -> isNetworkIdMatches(subnetIds, sn, cloudPlatform) || isNetworkNameMatches(subnetIds, sn, cloudPlatform))
                .collect(toMap(getNetworkIdentifier(cloudPlatform), Function.identity()));
    }

    private Function<? super CloudSubnet, ? extends String> getNetworkIdentifier(String cloudPlatform) {
        return isGcp(cloudPlatform) ? CloudSubnet::getName : CloudSubnet::getId;
    }

    private boolean isNetworkNameMatches(Set<String> subnetIds, CloudSubnet cs, String cloudPlatform) {
        return (subnetIds.contains(cs.getName()) || subnetIds.contains(cs.getId()))
                && isGcp(cloudPlatform);
    }

    private boolean isNetworkIdMatches(Set<String> subnetIds, CloudSubnet cs, String cloudPlatform) {
        return subnetIds.contains(cs.getId()) && !isGcp(cloudPlatform);
    }

    private boolean isAzure(String cloudPlatform) {
        return CloudPlatform.AZURE.name().equalsIgnoreCase(cloudPlatform);
    }

    private boolean isGcp(String cloudPlatform) {
        return CloudPlatform.GCP.name().equalsIgnoreCase(cloudPlatform);
    }

    private boolean isAws(String cloudPlatform) {
        return CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform);
    }

}
