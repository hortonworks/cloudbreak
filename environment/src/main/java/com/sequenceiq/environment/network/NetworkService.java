package com.sequenceiq.environment.network;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
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

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    public NetworkService(BaseNetworkRepository baseNetworkRepository,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap, CloudNetworkService cloudNetworkService,
            EnvironmentNetworkService environmentNetworkService, NetworkCreationValidator networkCreationValidator,
            RegionAwareCrnGenerator regionAwareCrnGenerator) {
        networkRepository = baseNetworkRepository;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.environmentNetworkService = environmentNetworkService;
        this.cloudNetworkService = cloudNetworkService;
        this.networkCreationValidator = networkCreationValidator;
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
    }

    public BaseNetwork saveNetwork(Environment environment, NetworkDto networkDto, String accountId, Map<String, CloudSubnet> subnetMetas,
            Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        BaseNetwork baseNetwork = null;
        if (networkDto != null) {
            EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
            if (environmentNetworkConverter != null) {
                baseNetwork = environmentNetworkConverter.convert(environment, networkDto, subnetMetas, endpointGatewaySubnetMetas);
                baseNetwork.setId(getIfNotNull(networkDto, NetworkDto::getId));
                baseNetwork.setResourceCrn(createCRN(accountId));
                baseNetwork.setAccountId(accountId);
                baseNetwork = save(baseNetwork);
            }
        }
        return baseNetwork;
    }

    private CloudPlatform getCloudPlatform(Environment environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

    public BaseNetwork validate(BaseNetwork originalNetwork, EnvironmentEditDto editDto, Environment environment) {
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (originalNetwork.getRegistrationType() == RegistrationType.CREATE_NEW) {
            throw new BadRequestException("Subnets of this environment could not be modified, because its network has been created by Cloudera. " +
                    "You need to re-install the environment into an existing VPC/VNet." + getDocLink(cloudPlatform));
        }
        EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(cloudPlatform);
        NetworkDto originalNetworkDto = environmentNetworkConverter.convertToDto(originalNetwork);
        NetworkDto cloneNetworkDto = NetworkDto.builder(originalNetworkDto)
                .withSubnetMetas(editDto.getNetworkDto().getSubnetMetas())
                .build();
        ValidationResultBuilder validationResultBuilder = networkCreationValidator.validateNetworkEdit(environment, cloneNetworkDto);
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        return originalNetwork;
    }

    public BaseNetwork refreshMetadataFromCloudProvider(BaseNetwork originalNetwork, EnvironmentEditDto editDto, Environment environment) {
        EnvironmentNetworkConverter environmentNetworkConverter = environmentNetworkConverterMap.get(CloudPlatform.valueOf(environment.getCloudPlatform()));
        NetworkDto originalNetworkDto = environmentNetworkConverter.convertToDto(originalNetwork);
        NetworkDto cloneNetworkDto = NetworkDto.builder(originalNetworkDto)
                .withSubnetMetas(editDto.getNetworkDto().getSubnetMetas())
                .build();

        try {
            Map<String, CloudSubnet> subnetMetadatas = cloudNetworkService.retrieveSubnetMetadata(environment, cloneNetworkDto);
            originalNetwork.setSubnetMetas(subnetMetadatas.values().stream().collect(toMap(CloudSubnet::getId, c -> c)));

            Map<String, CloudSubnet> endpointGatewaySubnetMetadatas =
                cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(environment, cloneNetworkDto);
            originalNetwork.setEndpointGatewaySubnetMetas(endpointGatewaySubnetMetadatas.values().stream().collect(toMap(CloudSubnet::getId, c -> c)));

            Network network = environmentNetworkConverter.convertToNetwork(originalNetwork);
            NetworkCidr networkCidr = environmentNetworkService.getNetworkCidr(network, environment.getCloudPlatform(), environment.getCredential());
            originalNetwork.setNetworkCidr(networkCidr.getCidr());
            originalNetwork.setNetworkCidrs(StringUtils.join(networkCidr.getCidrs(), ","));
        } catch (NetworkConnectorNotFoundException connectorNotFoundException) {
            throw new BadRequestException(connectorNotFoundException.getMessage());
        }
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
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.NETWORK, accountId);
    }

    private String getDocLink(CloudPlatform cloudPlatform) {
        String docReferenceLink = " Refer to Cloudera documentation at %s for more information.";
        switch (cloudPlatform) {
            case AWS:
                return String.format(docReferenceLink, DocumentationLinkProvider.awsAddSubnetLink());
            case AZURE:
                return String.format(docReferenceLink, DocumentationLinkProvider.azureAddSubnetLink());
            default:
                return "";
        }
    }

}
