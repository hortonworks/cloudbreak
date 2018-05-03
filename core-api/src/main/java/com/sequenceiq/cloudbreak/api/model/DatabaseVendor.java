package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsConfigRequestParameters;

public enum DatabaseVendor {
    POSTGRES("postgres", "postgres", "PostgreSQL", "PostgreSQL", "org.postgresql.Driver", "postgresql", ""),
    MYSQL("mysql", "mysql", "MySQL", "MySQL / MariaDB", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MARIADB("mysql", "mysql", "MariaDB", "MySQL / MariaDB", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MSSQL("mssql", "mssql", "SQLServer", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver", ""),
    ORACLE11("oracle", "oracle", "Oracle 11g", "Oracle", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc6.jar",
            Sets.newHashSet("oracle11g", "oracle11", "11", "11g")),
    ORACLE12("oracle", "oracle", "Oracle 12c", "Oracle", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc7.jar",
            Sets.newHashSet("oracle12c", "oracle12", "12", "12c")),
    SQLANYWHERE("sqlanywhere", "sqlanywhere", "SQLAnywhere", "SQL Anywhere", "org.postgresql.Driver", "sqlanywhere", ""),
    EMBEDDED("embedded", "embedded", "", "", "", "", "");

    private final String ambariVendor;
    private final String fancyName;
    private final String displayName;
    private final String connectionDriver;
    private final String jdbcUrlDriverId;
    private final String connectorJarName;
    private final String databaseType;
    private final Set<String> versions;

    DatabaseVendor(String ambariVendor, String databaseType, String displayName, String fancyName, String connectionDriver, String jdbcUrlDriverId,
            String connectorJarName, Set<String> versions) {
        this.ambariVendor = ambariVendor;
        this.databaseType = databaseType;
        this.fancyName = fancyName;
        this.displayName = displayName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
        this.connectorJarName = connectorJarName;
        this.versions = versions;
    }

    DatabaseVendor(String ambariVendor, String databaseType, String displayName, String fancyName, String connectionDriver, String jdbcUrlDriverId,
            String connectorJarName) {
        this(ambariVendor, databaseType, displayName, fancyName, connectionDriver, jdbcUrlDriverId, connectorJarName, Collections.emptySet());
    }

    public final String ambariVendor() {
        return ambariVendor;
    }

    public final String databaseType() {
        return databaseType;
    }

    public final String fancyName() {
        return fancyName;
    }

    public String connectionDriver() {
        return connectionDriver;
    }

    public String jdbcUrlDriverId() {
        return jdbcUrlDriverId;
    }

    public String connectorJarName() {
        return connectorJarName;
    }

    public String displayName() {
        return displayName;
    }

    public Set<String> versions() {
        return versions;
    }

    public static DatabaseVendor fromValue(String ambariVendor) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.ambariVendor.equals(ambariVendor)) {
                return vendor;
            }
        }
        throw new UnsupportedOperationException(String.format("%s is not a DatabaseVendor", ambariVendor));
    }

    public static Optional<DatabaseVendor> getVendorByJdbcUrl(RDSConfigRequest configRequest) {
        Optional<RdsConfigRequestParameters> rdsConfigRequestParameters = getParameters(configRequest);
        Optional<String> version = databaseVersion(rdsConfigRequestParameters);
        for (DatabaseVendor vendor : values()) {
            if (configRequest.getConnectionURL().startsWith(String.format("jdbc:%s:", vendor.jdbcUrlDriverId))) {
                if (vendor.versions.isEmpty()) {
                    return Optional.of(vendor);
                } else if (version.isPresent()) {
                    Optional<String> versionMatchFound = vendor.versions.stream().filter(item -> item.equals(version.get().toLowerCase())).findFirst();
                    if (versionMatchFound.isPresent()) {
                        return Optional.of(vendor);
                    }
                } else {
                    return Optional.of(vendor);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<RdsConfigRequestParameters> getParameters(RDSConfigRequest configRequest) {
        if (configRequest.getConnectionURL().contains(String.format("jdbc:%s:", ORACLE11.jdbcUrlDriverId))) {
            return Optional.ofNullable(configRequest.getOracleParameters());
        }
        return Optional.empty();
    }

    private static Optional<String> databaseVersion(Optional<RdsConfigRequestParameters> rdsConfigRequestParameters) {
        if (rdsConfigRequestParameters.isPresent()) {
            return Optional.of(rdsConfigRequestParameters.get().getVersion());
        } else {
            return Optional.empty();
        }
    }

    public static Collection<DatabaseVendor> availableVendors() {
        return EnumSet.complementOf(EnumSet.of(EMBEDDED));
    }

    public static Collection<DatabaseVendor> outOfTheBoxVendors() {
        return EnumSet.of(POSTGRES, MYSQL, MARIADB);
    }
}
