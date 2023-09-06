package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/*
Azure private DNS zones that can be registered and used by cloudbreak are listed below.
 */
public enum AzurePrivateDnsZoneServiceEnum implements AzurePrivateDnsZoneDescriptor {

    POSTGRES("Microsoft.DBforPostgreSQL/servers",
            "postgresqlServer",
            "privatelink.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzurePrivateDnsZoneServiceEnum.POSTGRES_DNS_ZONE_NAME_PATTERN),
    POSTGRES_FLEXIBLE("Microsoft.DBforPostgreSQL/flexibleServers",
            "flexiblePostgresqlServer",
            "postgres.database.azure.com",
            "postgres.database.azure.com",
            AzurePrivateDnsZoneServiceEnum.FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN),
    STORAGE("Microsoft.Storage/storageAccounts",
            "Blob",
            "privatelink.blob.core.windows.net",
            "blob.core.windows.net",
            AzurePrivateDnsZoneServiceEnum.STORAGE_DNS_ZONE_NAME_PATTERN);

    private static final String POSTGRES_DNS_ZONE_NAME_PATTERN = "privatelink\\.postgres\\.database\\.azure\\.com";

    // Each DNS label must start with a letter, end with a letter or digit, and have as interior
    // characters only letters, digits, and hyphen. It's length must be 63 characters or fewer
    // source: https://www.ietf.org/rfc/rfc1035.txt
    private static final String FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)*postgres\\.database\\.azure\\.com";

    private static final String STORAGE_DNS_ZONE_NAME_PATTERN = "privatelink\\.blob\\.core\\.windows\\.net";

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

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public String getSubResource() {
        return subResource;
    }

    @Override
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
