package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_EXISTING_DNS_ZONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.FLEXIBLE_POSTGRES_WITH_PE_AND_EXISTING_DNS_ZONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.POSTGRES_WITH_EXISTING_DNS_ZONE;
import static com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant.POSTGRES_WITH_NEW_DNS_ZONE;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.common.model.AzureDatabaseType;

/*
Azure private DNS zones that can be registered and used by Cloudbreak are listed below.
 */
public enum AzureManagedPrivateDnsZoneServiceType implements AzurePrivateDnsZoneDescriptor {

    POSTGRES(AzureResourceFamily.DATABASE,
            "Microsoft.DBforPostgreSQL/servers",
            AzureDatabaseType.SINGLE_SERVER.shortName(),
            "privatelink.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzureManagedPrivateDnsZoneServiceType.POSTGRES_DNS_ZONE_NAME_PATTERN,
            List.of(POSTGRES_WITH_EXISTING_DNS_ZONE, POSTGRES_WITH_NEW_DNS_ZONE)),

    POSTGRES_FLEXIBLE_FOR_PRIVATE_ENDPOINT(AzureResourceFamily.DATABASE,
            "Microsoft.DBforPostgreSQL/flexibleServers",
            AzureDatabaseType.FLEXIBLE_SERVER.shortName(),
            "privatelink.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzureManagedPrivateDnsZoneServiceType.POSTGRES_DNS_ZONE_NAME_PATTERN,
            List.of(FLEXIBLE_POSTGRES_WITH_PE_AND_EXISTING_DNS_ZONE, FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_EXISTING_DNS_ZONE)),

    POSTGRES_FLEXIBLE(AzureResourceFamily.DATABASE,
            "Microsoft.DBforPostgreSQL/flexibleServers",
            AzureDatabaseType.FLEXIBLE_SERVER.shortName(),
            "flexible.postgres.database.azure.com",
            "postgres.database.azure.com",
            AzureManagedPrivateDnsZoneServiceType.FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN,
            List.of(FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_EXISTING_DNS_ZONE, FLEXIBLE_POSTGRES_WITH_DELEGATED_SUBNET_AND_NEW_DNS_ZONE)) {

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
            AzureManagedPrivateDnsZoneServiceType.STORAGE_DNS_ZONE_NAME_PATTERN,
            List.of());

    private static final String POSTGRES_DNS_ZONE_NAME_PATTERN = "privatelink\\.postgres\\.database\\.azure\\.com";

    // Each DNS label must start with a letter, end with a letter or digit, and have as interior
    // characters only letters, digits, and hyphen. It's length must be 63 characters or fewer
    // source: https://www.ietf.org/rfc/rfc1035.txt
    private static final String FLEXIBLE_POSTGRES_DNS_ZONE_NAME_PATTERN = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+postgres\\.database\\.azure\\.com";

    private static final String STORAGE_DNS_ZONE_NAME_PATTERN = "privatelink\\.blob\\.core\\.windows\\.net";

    private static final ArrayListMultimap<String, AzureManagedPrivateDnsZoneServiceType> SERVICE_MAP_BY_RESOURCE;

    private final AzureResourceFamily resourceFamily;

    private final String resourceType;

    private final String subResource;

    private final String dnsZoneName;

    private final Pattern dnsZoneNamePattern;

    private final String dnsZoneForwarder;

    private final List<PrivateDatabaseVariant> supportedDatabaseVariants;

    AzureManagedPrivateDnsZoneServiceType(AzureResourceFamily resourceFamily, String resourceType, String subResource, String dnsZoneName,
            String dnsZoneForwarder, String dnsZoneNamePattern, List<PrivateDatabaseVariant> supportedVariants) {
        this.resourceFamily = resourceFamily;
        this.resourceType = resourceType;
        this.subResource = subResource;
        this.dnsZoneName = dnsZoneName;
        this.dnsZoneNamePattern = Pattern.compile(dnsZoneNamePattern);
        this.dnsZoneForwarder = dnsZoneForwarder;
        this.supportedDatabaseVariants = supportedVariants;
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

    public List<PrivateDatabaseVariant> getSupportedDatabaseVariants() {
        return supportedDatabaseVariants;
    }

    static {
        SERVICE_MAP_BY_RESOURCE = Stream.of(AzureManagedPrivateDnsZoneServiceType.values()).collect(
                Multimaps.toMultimap(
                                AzureManagedPrivateDnsZoneServiceType::getSubResource,
                                identity(),
                                ArrayListMultimap::create));
    }

    public static AzureManagedPrivateDnsZoneServiceType getBySubResourceAndVariant(String name, PrivateDatabaseVariant variant) {
        List<AzureManagedPrivateDnsZoneServiceType> types = SERVICE_MAP_BY_RESOURCE.get(name);
        if (types.size() ==  1) {
            return types.getFirst();
        } else {
            // orElse() should not happen as all variants are covered
            return types.stream()
                    .filter(type -> type.getSupportedDatabaseVariants().contains(variant))
                    .findFirst()
                    .orElse(POSTGRES_FLEXIBLE_FOR_PRIVATE_ENDPOINT);
        }
    }

    @Override
    public String toString() {
        return "AzureManagedPrivateDnsZoneServiceType{" +
                "resourceFamily=" + resourceFamily +
                ", resourceType='" + resourceType + '\'' +
                ", subResource='" + subResource + '\'' +
                ", dnsZoneName='" + dnsZoneName + '\'' +
                ", dnsZoneNamePattern=" + dnsZoneNamePattern +
                ", dnsZoneForwarder='" + dnsZoneForwarder + '\'' +
                ", supportedDatabaseVariants=" + supportedDatabaseVariants +
                "} " + super.toString();
    }
}