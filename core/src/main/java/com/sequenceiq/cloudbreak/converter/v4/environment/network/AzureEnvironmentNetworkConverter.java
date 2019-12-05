package com.sequenceiq.cloudbreak.converter.v4.environment.network;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Component
public class AzureEnvironmentNetworkConverter extends EnvironmentBaseNetworkConverter {

    @Override
    Map<String, Object> getAttributesForLegacyNetwork(EnvironmentNetworkResponse source) {
        EnvironmentNetworkAzureParams azure = source.getAzure();
        return Map.of(
                "networkId", azure.getNetworkId(),
                "resourceGroupName", azure.getResourceGroupName(),
                "noPublicIp", azure.getNoPublicIp());
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }
}
