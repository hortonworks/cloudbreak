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

    private static Set<SupportedExternalDatabaseServiceEntry> supportedExternalDatabases = new HashSet<>();

    private SupportedDatabaseProvider() {
    }

    static {
        supportedExternalDatabases.add(getSupportedServiceEntry("Hive", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Hive DAS", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Oozie", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Ranger", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Other", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Druid", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Superset", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Beacon", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Ambari", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Registry", MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Cloudera Manager", POSTGRES));
    }

    public static Set<SupportedExternalDatabaseServiceEntry> supportedExternalDatabases() {
        return supportedExternalDatabases;
    }

    public static Optional<SupportedExternalDatabaseServiceEntry> getOthers() {
        return supportedExternalDatabases.stream().filter(item -> item.getName().equals("Other".toUpperCase())).findFirst();
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
