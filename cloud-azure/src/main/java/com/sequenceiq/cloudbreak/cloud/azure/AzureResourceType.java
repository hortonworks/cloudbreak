package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.common.api.type.ResourceType;

public enum AzureResourceType {

    DATABASE_SERVER("DatabaseServer", ResourceType.AZURE_DATABASE),
    PRIVATE_ENDPOINT("PrivateEndpoint", ResourceType.AZURE_PRIVATE_ENDPOINT),
    PRIVATE_DNS_ZONE_GROUP("PrivateDnsZoneGroup", ResourceType.AZURE_DNS_ZONE_GROUP),
    RESOURCE_GROUP("ResourceGroup", ResourceType.AZURE_RESOURCE_GROUP);

    private static final Map<ResourceType, AzureResourceType> BY_RESOURCE_TYPE = Stream.of(AzureResourceType.values())
            .collect(Collectors.toUnmodifiableMap(AzureResourceType::getResourceType, Function.identity()));

    private final String azureType;

    private final ResourceType resourceType;

    AzureResourceType(String azureType, ResourceType resourceType) {
        this.azureType = azureType;
        this.resourceType = resourceType;
    }

    public String getAzureType() {
        return azureType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public static AzureResourceType getByResourceType(ResourceType resourceType) {
        return BY_RESOURCE_TYPE.get(resourceType);
    }
}
