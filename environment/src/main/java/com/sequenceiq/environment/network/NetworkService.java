package com.sequenceiq.environment.network;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dao.repository.BaseNetworkRepository;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Service
public class NetworkService {

    private final BaseNetworkRepository networkRepository;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final PlatformParameterService platformParameterService;

    private final EnvironmentNetworkService environmentNetworkService;

    public NetworkService(BaseNetworkRepository baseNetworkRepository,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            PlatformParameterService platformParameterService,
            EnvironmentNetworkService environmentNetworkService) {
        this.networkRepository = baseNetworkRepository;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.platformParameterService = platformParameterService;
        this.environmentNetworkService = environmentNetworkService;
    }

    public BaseNetwork saveNetwork(Environment environment, NetworkDto networkDto, String accountId, Map<String, CloudSubnet> subnetMetas) {
        BaseNetwork baseNetwork = null;
        if (networkDto != null) {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
            if (environmentNetworkConverter != null) {
                baseNetwork = environmentNetworkConverter.convert(environment, networkDto, subnetMetas);
                baseNetwork.setId(getIfNotNull(networkDto, NetworkDto::getId));
                baseNetwork.setResourceCrn(createCRN(accountId));
                baseNetwork.setAccountId(accountId);
                if (baseNetwork.getRegistrationType() == RegistrationType.EXISTING) {
                    Network network = environmentNetworkConverter.convertToNetwork(baseNetwork);
                    String networkCidr = environmentNetworkService.getNetworkCidr(network, environment.getCloudPlatform(), environment.getCredential());
                    baseNetwork.setNetworkCidr(networkCidr);
                }
                baseNetwork = save(baseNetwork);
            }
        }
        return baseNetwork;
    }

    private CloudPlatform getCloudPlatform(Environment environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

    public boolean hasExistingNetwork(BaseNetwork baseNetwork, CloudPlatform cloudPlatform) {
        return environmentNetworkConverterMap.get(cloudPlatform).hasExistingNetwork(baseNetwork);
    }

    public Map<String, CloudSubnet> retrieveSubnetMetadata(Environment environment, NetworkDto network) {
        if (network == null || network.getSubnetIds().isEmpty()) {
            return Map.of();
        } else if (AWS.equalsIgnoreCase(environment.getCloudPlatform())) {
            String regionName = environment.getRegionSet().iterator().next().getName();
            PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
            platformResourceRequest.setCredential(environment.getCredential());
            platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
            platformResourceRequest.setRegion(regionName);
            getAwsVpcId(network)
                    .ifPresent(vpcId -> platformResourceRequest.setFilters(Map.of("vpcId", vpcId)));

            CloudNetworks cloudNetworks = platformParameterService.getCloudNetworks(platformResourceRequest);
            Set<CloudNetwork> cloudNetworkSet = cloudNetworks.getCloudNetworkResponses().get(regionName);
            return cloudNetworkSet.stream()
                    .filter(n -> n.getId().equals(getAwsVpcId(network).orElse(null)))
                    .findFirst()
                    .map(CloudNetwork::getSubnetsMeta)
                    .stream()
                    .flatMap(Set::stream)
                    .filter(sn -> network.getSubnetIds().contains(sn.getId()))
                    .collect(Collectors.toMap(CloudSubnet::getId, Function.identity()));
        } else {
            return network.getSubnetIds().stream().collect(Collectors.toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    public Optional<String> getAwsVpcId(NetworkDto networkDto) {
        return Optional.ofNullable(networkDto)
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId);
    }

    public void delete(BaseNetwork network) {
        networkRepository.delete(network);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseNetwork> Optional<T> findByEnvironment(Long environmentId) {
        return networkRepository.findByEnvironmentId(environmentId);
    }

    @SuppressWarnings("unchecked")
    public BaseNetwork save(BaseNetwork network) {
        Object saved = networkRepository.save(network);
        return (BaseNetwork) saved;
    }

    private String createCRN(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.NETWORK)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

}
