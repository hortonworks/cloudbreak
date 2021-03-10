package com.sequenceiq.environment.network;

import static com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern.USE_MULTIPLE;

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
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkResourcesCreationRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
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
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

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
        NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform());
        NetworkCreationRequest networkCreationRequest = networkCreationRequestFactory.create(environment);
        EnvironmentNetworkConverter converter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
        CreatedCloudNetwork createdCloudNetwork = networkConnector.createNetworkWithSubnets(networkCreationRequest);
        return converter.setCreatedCloudNetwork(baseNetwork, createdCloudNetwork);
    }

    public void createProviderSpecificNetworkResources(EnvironmentDto environment, BaseNetwork baseNetwork) {
        NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform());
        NetworkResourcesCreationRequest networkResourcesCreationRequest =
                networkCreationRequestFactory.createProviderSpecificNetworkResources(environment, baseNetwork);
        networkConnector.createProviderSpecificNetworkResources(networkResourcesCreationRequest);
    }

    public NetworkCidr getNetworkCidr(Network network, String cloudPlatform, Credential credential) {
        if (network == null) {
            LOGGER.info("Could not fetch network cidr from {}, because the network is null", cloudPlatform);
            return null;
        }
        NetworkConnector networkConnector = getNetworkConnector(cloudPlatform);
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);
        return networkConnector.getNetworkCidr(network, cloudCredential);
    }

    public void deleteNetwork(EnvironmentDto environment) {
        try {
            NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform());
            networkConnector.deleteNetworkWithSubnets(createNetworkDeletionRequest(environment));
        } catch (NetworkConnectorNotFoundException connectorNotFoundException) {
            LOGGER.debug("Exiting from network deletion gracefully as Network connector couldn't be found for environment:'{}' and platform:'{}'",
                    environment.getName(), environment.getCloudPlatform());
        }
    }

    private NetworkDeletionRequest createNetworkDeletionRequest(EnvironmentDto environment) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        NetworkDeletionRequest.Builder builder = new NetworkDeletionRequest.Builder()
                .withStackName(networkCreationRequestFactory.getStackName(environment))
                .withCloudCredential(cloudCredential)
                .withRegion(environment.getLocation().getName())
                .withSingleResourceGroup(isSingleResourceGroup(environment))
                .withSubnetIds(environment.getNetwork().getSubnetIds())
                .withEnvName(environment.getName())
                .withEnvId(environment.getId())
                .withAccountId(environment.getAccountId())
                .withUserId(environment.getCreator())
                .withRegion(environment.getLocation().getName())
                .withNetworkId(getNetworkId(environment.getNetwork(), environment.getName()));
        getAzureResourceGroupName(environment).ifPresent(builder::withResourceGroup);
        builder.withExisting(environment.getNetwork().getRegistrationType() == RegistrationType.EXISTING);
        return builder.build();
    }

    private Optional<String> getAzureResourceGroupName(EnvironmentDto environmentDto) {
        if (isSingleResourceGroup(environmentDto)) {
            return Optional.of(environmentDto)
                    .map(EnvironmentDto::getParameters)
                    .map(ParametersDto::getAzureParametersDto)
                    .map(AzureParametersDto::getAzureResourceGroupDto)
                    .map(AzureResourceGroupDto::getName);
        } else {
            return Optional.of(environmentDto)
                    .map(EnvironmentDto::getNetwork)
                    .map(NetworkDto::getAzure)
                    .map(AzureParams::getResourceGroupName);
        }
    }

    private String getNetworkId(NetworkDto networkDto, String envName) {
        String networkId = networkDto.getNetworkId();
        if (networkDto.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AZURE.name())) {
            networkId = Optional.of(networkDto)
                    .map(NetworkDto::getAzure)
                    .map(AzureParams::getNetworkId)
                    .orElse(envName);
        }
        return networkId;
    }

    private boolean isSingleResourceGroup(EnvironmentDto environmentDto) {
        ResourceGroupUsagePattern resourceGroupUsagePattern = Optional.ofNullable(environmentDto.getParameters())
                .map(ParametersDto::azureParametersDto)
                .map(AzureParametersDto::getAzureResourceGroupDto)
                .map(AzureResourceGroupDto::getResourceGroupUsagePattern)
                .orElse(USE_MULTIPLE);
        return resourceGroupUsagePattern.isSingleResourceGroup();
    }

    private NetworkConnector getNetworkConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).networkConnector())
                .orElseThrow(() -> new NetworkConnectorNotFoundException("No network connector for cloud platform: " + cloudPlatform));
    }

    private CloudPlatform getCloudPlatform(EnvironmentDto environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }
}
