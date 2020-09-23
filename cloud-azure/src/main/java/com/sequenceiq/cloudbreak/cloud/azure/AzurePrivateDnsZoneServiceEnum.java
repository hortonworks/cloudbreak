package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

public enum AzurePrivateDnsZoneServiceEnum {

    POSTGRES("Microsoft.DBforPostgreSQL/servers", "postgresqlServer", "privatelink.postgres.database.azure.com", "postgres.database.azure.com"),
    STORAGE("Microsoft.Storage/storageAccounts", "Blob", "privatelink.blob.core.windows.net", "blob.core.windows.net");

    private static final Map<String, AzurePrivateDnsZoneServiceEnum> SERVICE_MAP_BY_RESOURCE;

    private final String resourceType;

    private final String subResource;

    private final String dnsZoneName;

    private final String dnsZoneForwarder;

    AzurePrivateDnsZoneServiceEnum(String resourceType, String subResource, String dnsZoneName, String dnsZoneForwarder) {
        this.resourceType = resourceType;
        this.subResource = subResource;
        this.dnsZoneName = dnsZoneName;
        this.dnsZoneForwarder = dnsZoneForwarder;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getSubResource() {
        return subResource;
    }

    public String getDnsZoneName() {
        return dnsZoneName;
    }

    public String getDnsZoneForwarder() {
        return dnsZoneForwarder;
    }

    static {
        SERVICE_MAP_BY_RESOURCE = Stream.of(AzurePrivateDnsZoneServiceEnum.values()).collect(toMap(AzurePrivateDnsZoneServiceEnum::getSubResource, identity()));
    }

    public static AzurePrivateDnsZoneServiceEnum getBySubResource(String name) {
        return SERVICE_MAP_BY_RESOURCE.get(name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AzurePrivateDnsZoneServiceEnum.class.getSimpleName() + "[", "]")
                .add("resourceType='" + resourceType + "'")
                .add("subResource='" + subResource + "'")
                .add("dnsZoneName='" + dnsZoneName + "'")
                .add("dnsZoneForwarder='" + dnsZoneForwarder + "'")
                .toString();
    }
}
