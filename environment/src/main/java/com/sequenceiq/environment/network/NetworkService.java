package com.sequenceiq.environment.network;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
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

    private final NetworkCreationValidator networkCreationValidator;

    public NetworkService(BaseNetworkRepository baseNetworkRepository,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            PlatformParameterService platformParameterService,
            EnvironmentNetworkService environmentNetworkService, NetworkCreationValidator networkCreationValidator) {
        this.networkRepository = baseNetworkRepository;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.platformParameterService = platformParameterService;
        this.environmentNetworkService = environmentNetworkService;
        this.networkCreationValidator = networkCreationValidator;
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

    public Map<String, CloudSubnet> retrieveSubnetMetadata(Environment environment, NetworkDto network) {
        if (network == null || network.getSubnetIds().isEmpty()) {
            return Map.of();
        } else if (isAws(environment)) {
            Map<String, String> filter = getAwsVpcId(network)
                    .map(vpcId -> Map.of("vpcId", vpcId))
                    .orElse(Map.of());
            return fetchCloudNetwork(environment, network, filter);
        } else if (isAzure(environment)) {
            Map<String, String> filter = new HashMap<>();
            filter.put("networkId", network.getAzure().getNetworkId());
            filter.put("resourceGroupName", network.getAzure().getResourceGroupName());
            return fetchCloudNetwork(environment, network, filter);
        } else {
            return network.getSubnetIds().stream().collect(toMap(Function.identity(), id -> new CloudSubnet(id, null)));
        }
    }

    private Map<String, CloudSubnet> fetchCloudNetwork(Environment environment, NetworkDto network, Map<String, String> filter) {
        String regionName = environment.getRegionSet().iterator().next().getName();
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        platformResourceRequest.setCredential(environment.getCredential());
        platformResourceRequest.setCloudPlatform(environment.getCloudPlatform());
        platformResourceRequest.setRegion(regionName);
        platformResourceRequest.setFilters(filter);

        CloudNetworks cloudNetworks = platformParameterService.getCloudNetworks(platformResourceRequest);
        Set<CloudNetwork> cloudNetworkSet = cloudNetworks.getCloudNetworkResponses().get(regionName);
        return cloudNetworkSet.stream()
                .flatMap(it -> it.getSubnetsMeta().stream())
                .filter(sn -> network.getSubnetIds().contains(sn.getId()))
                .collect(toMap(CloudSubnet::getId, Function.identity()));
    }

    private boolean isAzure(Environment environment) {
        return CloudPlatform.AZURE.name().equalsIgnoreCase(environment.getCloudPlatform());
    }

    private boolean isAws(Environment environment) {
        return CloudPlatform.AWS.name().equalsIgnoreCase(environment.getCloudPlatform());
    }

    public Optional<String> getAwsVpcId(NetworkDto networkDto) {
        return Optional.ofNullable(networkDto)
                .map(NetworkDto::getAws)
                .map(AwsParams::getVpcId);
    }

    public BaseNetwork validateAndReplaceSubnets(BaseNetwork originalNetwork, EnvironmentEditDto editDto, Environment environment) {
        if (originalNetwork.getRegistrationType() == RegistrationType.CREATE_NEW) {
            throw new BadRequestException("Subnet could not be attached to this environment, because it is newly created by Cloudbreak. " +
                    "You need to re-install the the environment into an existing VPC");
        }
        EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()));
        NetworkDto originalNetworkDto = environmentNetworkConverter.convertToDto(originalNetwork);
        NetworkDto cloneNetworkDto = NetworkDto.builder(originalNetworkDto)
                .withSubnetMetas(editDto.getNetworkDto().getSubnetMetas())
                .build();
        Map<String, CloudSubnet> subnetMetadatas = retrieveSubnetMetadata(environment, cloneNetworkDto);
        ValidationResult.ValidationResultBuilder validationResultBuilder = networkCreationValidator.validateNetworkEdit(environment, cloneNetworkDto,
                subnetMetadatas);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        originalNetwork.setSubnetMetas(subnetMetadatas.values().stream().collect(toMap(CloudSubnet::getId, c -> c)));
        return originalNetwork;
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
