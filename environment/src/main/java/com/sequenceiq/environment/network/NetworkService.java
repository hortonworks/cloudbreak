package com.sequenceiq.environment.network;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dao.repository.BaseNetworkRepository;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Service
public class NetworkService {

    private final BaseNetworkRepository networkRepository;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final EnvironmentNetworkService environmentNetworkService;

    private final NetworkCreationValidator networkCreationValidator;

    private final CloudNetworkService cloudNetworkService;

    public NetworkService(BaseNetworkRepository baseNetworkRepository,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap, CloudNetworkService cloudNetworkService,
            EnvironmentNetworkService environmentNetworkService, NetworkCreationValidator networkCreationValidator) {
        networkRepository = baseNetworkRepository;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentNetworkService = environmentNetworkService;
        this.cloudNetworkService = cloudNetworkService;
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
        ValidationResultBuilder validationResultBuilder = networkCreationValidator.validateNetworkEdit(environment, cloneNetworkDto);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        Map<String, CloudSubnet> subnetMetadatas = cloudNetworkService.retrieveSubnetMetadata(environment, cloneNetworkDto);
        originalNetwork.setSubnetMetas(subnetMetadatas.values().stream().collect(toMap(CloudSubnet::getId, c -> c)));
        return originalNetwork;
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
                .setResourceType(ResourceType.NETWORK)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

}
