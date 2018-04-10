package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.model.DatabaseVendor.POSTGRES;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.SupportedDatabaseEntry;
import com.sequenceiq.cloudbreak.cloud.model.SupportedExternalDatabaseServiceEntry;

@Component
public class SupportedDatabaseProvider {

    private Set<SupportedExternalDatabaseServiceEntry> supportedExternalDatabases = new HashSet<>();

    @PostConstruct
    private void init() {
        this.supportedExternalDatabases = initSupportedExternalDatabases();
    }

    private Set<SupportedExternalDatabaseServiceEntry> initSupportedExternalDatabases() {
        supportedExternalDatabases.add(getSupportedServiceEntry("Hive", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Oozie", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Ranger", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Others", POSTGRES, MYSQL, ORACLE11, ORACLE12));
        supportedExternalDatabases.add(getSupportedServiceEntry("Druid", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Superset", POSTGRES, MYSQL));
        supportedExternalDatabases.add(getSupportedServiceEntry("Ambari", POSTGRES, MYSQL));
        return supportedExternalDatabases;
    }

    private SupportedExternalDatabaseServiceEntry getSupportedServiceEntry(String name, DatabaseVendor... vendors) {
        SupportedExternalDatabaseServiceEntry entry = new SupportedExternalDatabaseServiceEntry();
        entry.setName(name);
        entry.setDisplayName(name.toUpperCase());
        for (DatabaseVendor vendor : vendors) {
            entry.getDatabases().add(new SupportedDatabaseEntry(vendor.name(), vendor.fancyName(), vendor.jdbcUrlDriverId()));
        }
        return entry;
    }

    public Set<SupportedExternalDatabaseServiceEntry> get() {
        return this.supportedExternalDatabases;
    }
}
