package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.common.api.type.ResourceType;

public enum AzureResourceType {

    DATABASE_SERVER("DatabaseServer", List.of(ResourceType.AZURE_DATABASE, ResourceType.AZURE_DATABASE_CANARY)),
    PRIVATE_ENDPOINT("PrivateEndpoint",  List.of(ResourceType.AZURE_PRIVATE_ENDPOINT, ResourceType.AZURE_PRIVATE_ENDPOINT_CANARY)),
    PRIVATE_DNS_ZONE_GROUP("PrivateDnsZoneGroup",  List.of(ResourceType.AZURE_DNS_ZONE_GROUP, ResourceType.AZURE_DNS_ZONE_GROUP_CANARY)),
    RESOURCE_GROUP("ResourceGroup", ResourceType.AZURE_RESOURCE_GROUP);

    private static final Map<ResourceType, AzureResourceType> BY_RESOURCE_TYPE = Stream.of(AzureResourceType.values())
            .flatMap(e -> e.resourceTypes.stream().map(s -> Map.entry(s, e)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private final String azureType;

    private final List<ResourceType> resourceTypes;

    AzureResourceType(String azureType, ResourceType resourceType) {
        this.azureType = azureType;
        this.resourceTypes = List.of(resourceType);
    }

    AzureResourceType(String azureType, List<ResourceType> resourceTypes) {
        this.azureType = azureType;
        this.resourceTypes = resourceTypes;
    }

    public String getAzureType() {
        return azureType;
    }

    public List<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public static AzureResourceType getByResourceType(ResourceType resourceType) {
        return BY_RESOURCE_TYPE.get(resourceType);
    }
}