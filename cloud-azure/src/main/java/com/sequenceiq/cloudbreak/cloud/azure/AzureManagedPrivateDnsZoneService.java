package com.sequenceiq.cloudbreak.cloud.azure;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.sequenceiq.common.model.AzureDatabaseType;

/*
Azure private DNS zones that can be registered and used by Cloudbreak are listed below.
 */
public enum AzureManagedPrivateDnsZoneService implements AzurePrivateDnsZoneDescriptor {

    POSTGRES(AzureResourceFamily.DATABASE,
            "Microsoft.DBforPostgreSQL/servers",
            AzureDatabaseType.SINGLE_SERVER.shortName(),
            "privatelink.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzureManagedPrivateDnsZoneService.POSTGRES_DNS_ZONE_NAME_PATTERN),
    POSTGRES_FLEXIBLE(AzureResourceFamily.DATABASE,
            "Microsoft.DBforPostgreSQL/flexibleServers",
            AzureDatabaseType.FLEXIBLE_SERVER.shortName(),
            "flexible.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzureManagedPrivateDnsZoneService.FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN) {

        @Override
        public String getDnsZoneName(String resourceGroupName) {
            return String.join(".", resourceGroupName, POSTGRES_FLEXIBLE.dnsZoneName);
        }
    },
    STORAGE(AzureResourceFamily.STORAGE,
            "Microsoft.Storage/storageAccounts",
            "Blob",
            "privatelink.blob.core.windows.net",
            "blob.core.windows.net",
            AzureManagedPrivateDnsZoneService.STORAGE_DNS_ZONE_NAME_PATTERN);

    private static final String POSTGRES_DNS_ZONE_NAME_PATTERN = "privatelink\\.postgres\\.database\\.azure\\.com";

    // Each DNS label must start with a letter, end with a letter or digit, and have as interior
    // characters only letters, digits, and hyphen. It's length must be 63 characters or fewer
    // source: https://www.ietf.org/rfc/rfc1035.txt
    private static final String FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+postgres\\.database\\.azure\\.com";

    private static final String STORAGE_DNS_ZONE_NAME_PATTERN = "privatelink\\.blob\\.core\\.windows\\.net";

    private static final Map<String, AzureManagedPrivateDnsZoneService> SERVICE_MAP_BY_RESOURCE;

    private final AzureResourceFamily resourceFamily;

    private final String resourceType;

    private final String subResource;

    private final String dnsZoneName;

    private final Pattern dnsZoneNamePattern;

    private final String dnsZoneForwarder;

    AzureManagedPrivateDnsZoneService(AzureResourceFamily resourceFamily, String resourceType, String subResource, String dnsZoneName,
            String dnsZoneForwarder, String dnsZoneNamePattern) {
        this.resourceFamily = resourceFamily;
        this.resourceType = resourceType;
        this.subResource = subResource;
        this.dnsZoneName = dnsZoneName;
        this.dnsZoneNamePattern = Pattern.compile(dnsZoneNamePattern);
        this.dnsZoneForwarder = dnsZoneForwarder;
    }

    public AzureResourceFamily getResourceFamily() {
        return resourceFamily;
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
    public String getDnsZoneName(String resourceGroupName) {
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
        SERVICE_MAP_BY_RESOURCE = Stream.of(AzureManagedPrivateDnsZoneService.values()).collect(
                toMap(AzureManagedPrivateDnsZoneService::getSubResource, identity()));
    }

    public static AzureManagedPrivateDnsZoneService getBySubResource(String name) {
        return SERVICE_MAP_BY_RESOURCE.get(name);
    }

    @Override
    public String toString() {
        return "AzureManagedPrivateDnsZoneService{" +
                "resourceType='" + resourceType + '\'' +
                ", subResource='" + subResource + '\'' +
                ", dnsZoneName='" + dnsZoneName + '\'' +
                ", dnsZoneNamePattern=" + dnsZoneNamePattern +
                ", dnsZoneForwarder='" + dnsZoneForwarder + '\'' +
                "} " + super.toString();
    }
}