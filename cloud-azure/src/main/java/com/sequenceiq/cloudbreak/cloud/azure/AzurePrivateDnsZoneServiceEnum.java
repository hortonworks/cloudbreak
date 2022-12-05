package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum AzurePrivateDnsZoneServiceEnum implements AzurePrivateDnsZoneDescriptor {

    POSTGRES("Microsoft.DBforPostgreSQL/servers", "postgresqlServer", "privatelink.postgres.database.azure.com",
            "postgres.database.azure.com", "privatelink\\.postgres\\.database\\.azure\\.com"),
    STORAGE("Microsoft.Storage/storageAccounts", "Blob", "privatelink.blob.core.windows.net",
            "blob.core.windows.net", "privatelink\\.blob\\.core\\.windows\\.net");

    private static final Map<String, AzurePrivateDnsZoneServiceEnum> SERVICE_MAP_BY_RESOURCE;

    private final String resourceType;

    private final String subResource;

    private final String dnsZoneName;

    private final Pattern dnsZoneNamePattern;

    private final String dnsZoneForwarder;

    AzurePrivateDnsZoneServiceEnum(String resourceType, String subResource, String dnsZoneName, String dnsZoneForwarder, String dnsZoneNamePattern) {
        this.resourceType = resourceType;
        this.subResource = subResource;
        this.dnsZoneName = dnsZoneName;
        this.dnsZoneNamePattern = Pattern.compile(dnsZoneNamePattern);
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

    @Override
    public List<Pattern> getDnsZoneNamePatterns() {
        return List.of(dnsZoneNamePattern);
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
        return "AzurePrivateDnsZoneServiceEnum{" +
                "resourceType='" + resourceType + '\'' +
                ", subResource='" + subResource + '\'' +
                ", dnsZoneName='" + dnsZoneName + '\'' +
                ", dnsZoneNamePattern=" + dnsZoneNamePattern +
                ", dnsZoneForwarder='" + dnsZoneForwarder + '\'' +
                "} " + super.toString();
    }

}
