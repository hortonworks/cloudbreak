package com.sequenceiq.environment.network;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentNetworkManagementService {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final NetworkCreationRequestFactory networkCreationRequestFactory;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    private final CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    public EnvironmentNetworkManagementService(
            CloudPlatformConnectors cloudPlatformConnectors,
            NetworkCreationRequestFactory networkCreationRequestFactory,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap,
            CredentialToCloudCredentialConverter credentialToCloudCredentialConverter) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.networkCreationRequestFactory = networkCreationRequestFactory;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
        this.credentialToCloudCredentialConverter = credentialToCloudCredentialConverter;
    }

    public BaseNetwork createNetwork(EnvironmentDto environment, BaseNetwork baseNetwork) {
        NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform());
        NetworkCreationRequest networkCreationRequest = networkCreationRequestFactory.create(environment);
        EnvironmentNetworkConverter converter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
        CreatedCloudNetwork createdCloudNetwork = networkConnector.createNetworkWithSubnets(networkCreationRequest);
        return converter.setProviderSpecificNetwork(baseNetwork, createdCloudNetwork);
    }

    public void deleteNetwork(EnvironmentDto environment) {
        NetworkConnector networkConnector = getNetworkConnector(environment.getCloudPlatform());
        networkConnector.deleteNetworkWithSubnets(createNetworkDeletionRequest(environment));
    }

    private NetworkDeletionRequest createNetworkDeletionRequest(EnvironmentDto environment) {
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(environment.getCredential());
        NetworkDeletionRequest.Builder builder = new NetworkDeletionRequest.Builder()
                .withStackName(environment.getNetwork().getNetworkName())
                .withCloudCredential(cloudCredential)
                .withRegion(environment.getLocation().getName());
        getNoPublicIp(environment.getNetwork()).ifPresent(builder::withResourceGroup);
        return builder.build();
    }

    private Optional<String> getNoPublicIp(NetworkDto networkDto) {
        return Optional.of(networkDto).map(NetworkDto::getAzure).map(AzureParams::getResourceGroupName);
    }

    private NetworkConnector getNetworkConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return cloudPlatformConnectors.get(cloudPlatformVariant).networkConnector();
    }

    private CloudPlatform getCloudPlatform(EnvironmentDto environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

}
