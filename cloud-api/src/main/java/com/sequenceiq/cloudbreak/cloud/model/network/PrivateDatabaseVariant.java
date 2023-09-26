package com.sequenceiq.cloudbreak.cloud.model.network;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static java.util.Map.entry;

import java.util.Map;

import com.sequenceiq.common.model.AzureDatabaseType;

public enum PrivateDatabaseVariant {

    NONE(false, null),
    POSTGRES_WITH_EXISTING_DNS_ZONE(false, SINGLE_SERVER),
    POSTGRES_WITH_NEW_DNS_ZONE(true, SINGLE_SERVER),
    FLEXIBLE_POSTGRES_WITH_EXISTING_DNS_ZONE(false, FLEXIBLE_SERVER),
    FLEXIBLE_POSTGRES_WITH_NEW_DNS_ZONE(true, FLEXIBLE_SERVER);

    private static final Map<PrivateDatabaseVariantKey, PrivateDatabaseVariant> VARIANT_MAP = Map.ofEntries(
            entry(new PrivateDatabaseVariantKey(false, false, false), NONE),
            entry(new PrivateDatabaseVariantKey(false, true, false), NONE),
            entry(new PrivateDatabaseVariantKey(true, false, true), NONE),
            entry(new PrivateDatabaseVariantKey(true, true, true), NONE),
            entry(new PrivateDatabaseVariantKey(false, false, true), FLEXIBLE_POSTGRES_WITH_NEW_DNS_ZONE),
            entry(new PrivateDatabaseVariantKey(false, true, true), FLEXIBLE_POSTGRES_WITH_EXISTING_DNS_ZONE),
            entry(new PrivateDatabaseVariantKey(true, false, false), POSTGRES_WITH_NEW_DNS_ZONE),
            entry(new PrivateDatabaseVariantKey(true, true, false), POSTGRES_WITH_EXISTING_DNS_ZONE));

    private final boolean zoneManagedByCdp;

    private final AzureDatabaseType databaseType;

    PrivateDatabaseVariant(boolean zoneManagedByCdp, AzureDatabaseType databaseType) {
        this.zoneManagedByCdp = zoneManagedByCdp;
        this.databaseType = databaseType;
    }

    public boolean isZoneManagedByCdp() {
        return zoneManagedByCdp;
    }

    public AzureDatabaseType getDatabaseType() {
        return databaseType;
    }

    public static PrivateDatabaseVariant fromPrivateEndpointSettings(boolean hasPrivateEndpointEnabled,
            boolean hasExistingDnsZone,
            boolean hasFlexibleServerSubnets) {
        PrivateDatabaseVariantKey key = new PrivateDatabaseVariantKey(
                hasPrivateEndpointEnabled,
                hasExistingDnsZone,
                hasFlexibleServerSubnets);

        PrivateDatabaseVariant variant = VARIANT_MAP.get(key);
        if (variant != null) {
            return variant;
        }
        // is not possible as all combinations are covered
        throw new IllegalArgumentException("Invalid combination of settings");
    }

    private static class PrivateDatabaseVariantKey {

        private final boolean hasPrivateEndpointEnabled;

        private final boolean hasExistingDnsZone;

        private final boolean hasFlexibleServerSubnets;

        private PrivateDatabaseVariantKey(boolean hasPrivateEndpointEnabled, boolean hasExistingDnsZone,
                boolean hasFlexibleServerSubnets) {
            this.hasPrivateEndpointEnabled = hasPrivateEndpointEnabled;
            this.hasExistingDnsZone = hasExistingDnsZone;
            this.hasFlexibleServerSubnets = hasFlexibleServerSubnets;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            PrivateDatabaseVariantKey that = (PrivateDatabaseVariantKey) o;
            if (hasPrivateEndpointEnabled != that.hasPrivateEndpointEnabled) {
                return false;
            }
            if (hasExistingDnsZone != that.hasExistingDnsZone) {
                return false;
            }
            return hasFlexibleServerSubnets == that.hasFlexibleServerSubnets;
        }

        @Override
        public int hashCode() {
            int result = (hasPrivateEndpointEnabled ? 1 : 0);
            result = 31 * result + (hasExistingDnsZone ? 1 : 0);
            result = 31 * result + (hasFlexibleServerSubnets ? 1 : 0);
            return result;
        }
    }
}