package com.sequenceiq.environment.network;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentNetworkService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentNetworkService.class);

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final NetworkCreationRequestFactory networkCreationRequestFactory;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public EnvironmentNetworkService(
            CloudPlatformConnectors cloudPlatformConnectors,
            NetworkCreationRequestFactory networkCreationRequestFactory,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.networkCreationRequestFactory = networkCreationRequestFactory;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public BaseNetwork createCloudNetwork(EnvironmentDto environment, BaseNetwork baseNetwork) {
        NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform())
                .orElseThrow(() -> new BadRequestException("No network connector for cloud platform: " + environment.getCloudPlatform()));
        NetworkCreationRequest networkCreationRequest = networkCreationRequestFactory.create(environment);
        EnvironmentNetworkConverter converter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
        CreatedCloudNetwork createdCloudNetwork = networkConnector.createNetworkWithSubnets(networkCreationRequest);
        return converter.setCreatedCloudNetwork(baseNetwork, createdCloudNetwork);
    }

    public NetworkCidr getNetworkCidr(Network network, String cloudPlatform, Credential credential) {
        if (network == null) {
            LOGGER.info("Could not fetch network cidr from {}, because the network is null", cloudPlatform);
            return null;
        }
        NetworkConnector networkConnector = getNetworkConnector(cloudPlatform)
                .orElseThrow(() -> new BadRequestException("No network connector for cloud platform: " + cloudPlatform));
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        return networkConnector.getNetworkCidr(network, cloudCredential);
    }

    public void deleteNetwork(EnvironmentDto environment) {
        getNetworkConnector(environment.getCloudPlatform())
                .ifPresentOrElse(connector -> connector.deleteNetworkWithSubnets(createNetworkDeletionRequest(environment)),
                        () -> LOGGER.info("No network connector for cloud platform: {}", environment.getCloudPlatform()));
    }

    private NetworkDeletionRequest createNetworkDeletionRequest(EnvironmentDto environment) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        NetworkDeletionRequest.Builder builder = new NetworkDeletionRequest.Builder()
                .withStackName(networkCreationRequestFactory.getStackName(environment))
                .withCloudCredential(cloudCredential)
                .withExisting(environment.getNetwork().getRegistrationType().equals(RegistrationType.EXISTING))
                .withSubnetIds(environment.getNetwork().getSubnetIds())
                .withNetworkId(environment.getNetwork().getNetworkId())
                .withEnvName(environment.getName())
                .withEnvId(environment.getId())
                .withAccountId(environment.getAccountId())
                .withUserId(environment.getCreator())
                .withRegion(environment.getLocation().getName());
        getResourceGroupName(environment.getNetwork()).ifPresent(builder::withResourceGroup);
        builder.withExisting(environment.getNetwork().getRegistrationType() == RegistrationType.EXISTING);
        return builder.build();
    }

    private Optional<String> getResourceGroupName(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::getResourceGroupName);
    }

    private Optional<NetworkConnector> getNetworkConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).networkConnector());
    }

    private CloudPlatform getCloudPlatform(EnvironmentDto environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }
}
