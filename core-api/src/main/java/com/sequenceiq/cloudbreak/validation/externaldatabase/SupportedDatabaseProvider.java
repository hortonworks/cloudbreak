package com.sequenceiq.cloudbreak.validation.externaldatabase;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.POSTGRES;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

public final class SupportedDatabaseProvider {

    private static final Set<SupportedExternalDatabaseServiceEntry> SUPPORTED_EXTERNAL_DATABASES = new HashSet<>();

    private SupportedDatabaseProvider() {
    }

    static {
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Hive", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Hive DAS", POSTGRES));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Hue Query Processor", POSTGRES));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Oozie", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Ranger", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Other", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Druid", POSTGRES, MYSQL));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Superset", POSTGRES, MYSQL));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Beacon", POSTGRES, MYSQL));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Ambari", POSTGRES, MYSQL));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Registry", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Streams Messaging Manager", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Hue", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Cloudera Manager", POSTGRES));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Knox Gateway", POSTGRES));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Cloudera Manager Management Service Activity Monitor", POSTGRES));
        SUPPORTED_EXTERNAL_DATABASES.add(getSupportedServiceEntry("Cloudera Manager Management Service Reports Manager", POSTGRES));
    }

    public static Set<SupportedExternalDatabaseServiceEntry> supportedExternalDatabases() {
        return SUPPORTED_EXTERNAL_DATABASES;
    }

    public static Optional<SupportedExternalDatabaseServiceEntry> getOthers() {
        return SUPPORTED_EXTERNAL_DATABASES.stream().filter(item -> item.getName().equals("Other".toUpperCase())).findFirst();
    }

    private static SupportedExternalDatabaseServiceEntry getSupportedServiceEntry(String name, DatabaseVendor... vendors) {
        SupportedExternalDatabaseServiceEntry entry = new SupportedExternalDatabaseServiceEntry();
        entry.setName(name.toUpperCase().replaceAll(" ", "_"));
        entry.setDisplayName(name);
        for (DatabaseVendor databaseVendor : vendors) {
            entry.getDatabases().add(new SupportedDatabaseEntry(databaseVendor.name(), databaseVendor.displayName(),
                    databaseVendor.jdbcUrlDriverId(), databaseVendor.versions()));
        }
        return entry;
    }
}
