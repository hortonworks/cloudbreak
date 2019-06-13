package com.sequenceiq.environment.network;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.service.NetworkCreationRequestFactory;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@Component
public class EnvironmentNetworkCreationService {

    private final CloudPlatformConnectors cloudPlatformConnectors;

    private final NetworkCreationRequestFactory networkCreationRequestFactory;

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    public EnvironmentNetworkCreationService(
            CloudPlatformConnectors cloudPlatformConnectors,
            NetworkCreationRequestFactory networkCreationRequestFactory,
            Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap) {
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.networkCreationRequestFactory = networkCreationRequestFactory;
        this.environmentNetworkConverterMap = environmentNetworkConverterMap;
    }

    public BaseNetwork createNetwork(EnvironmentDto environment, BaseNetwork baseNetwork) {
        NetworkConnector networkConnector = getNetworkConnector(environment);
        NetworkCreationRequest networkCreationRequest = networkCreationRequestFactory.create(environment);
        EnvironmentNetworkConverter converter = environmentNetworkConverterMap.get(getCloudPlatform(environment));
        CreatedCloudNetwork createdCloudNetwork = networkConnector.createNetworkWithSubnets(networkCreationRequest);
        return converter.setProviderSpecificNetwork(baseNetwork, createdCloudNetwork);
    }

    private NetworkConnector getNetworkConnector(EnvironmentDto environment) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(
                Platform.platform(environment.getCloudPlatform()), Variant.variant(environment.getCloudPlatform()));
        return cloudPlatformConnectors.get(cloudPlatformVariant).networkConnector();
    }

    private CloudPlatform getCloudPlatform(EnvironmentDto environment) {
        return CloudPlatform.valueOf(environment.getCloudPlatform());
    }

}
