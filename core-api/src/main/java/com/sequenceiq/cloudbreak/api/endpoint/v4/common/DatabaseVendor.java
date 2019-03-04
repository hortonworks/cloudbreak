package com.sequenceiq.cloudbreak.api.endpoint.v4.common;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseRequestParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;

public enum DatabaseVendor {
    POSTGRES("postgres", "PostgreSQL", "PostgreSQL", "org.postgresql.Driver", "postgresql", ""),
    MYSQL("mysql", "MySQL", "MySQL / MariaDB", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MARIADB("mysql", "MariaDB", "MySQL / MariaDB", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MSSQL("mssql", "SQLServer", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver", ""),
    ORACLE11("oracle", "Oracle 11g", "Oracle", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc6.jar", newHashSet("oracle11g", "oracle11", "11", "11g")),
    ORACLE12("oracle", "Oracle 12c", "Oracle", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc7.jar", newHashSet("oracle12c", "oracle12", "12", "12c")),
    SQLANYWHERE("sqlanywhere", "SQLAnywhere", "SQL Anywhere", "org.postgresql.Driver", "sqlanywhere", ""),
    EMBEDDED("embedded", "", "", "", "", "");

    private final String fancyName;
    private final String displayName;
    private final String connectionDriver;
    private final String jdbcUrlDriverId;
    private final String connectorJarName;
    private final String databaseType;
    private final Set<String> versions;

    DatabaseVendor(String databaseType, String displayName, String fancyName, String connectionDriver, String jdbcUrlDriverId,
            String connectorJarName, Set<String> versions) {
        this.databaseType = databaseType;
        this.fancyName = fancyName;
        this.displayName = displayName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
        this.connectorJarName = connectorJarName;
        this.versions = versions;
    }

    DatabaseVendor(String databaseType, String displayName, String fancyName, String connectionDriver, String jdbcUrlDriverId,
            String connectorJarName) {
        this(databaseType, displayName, fancyName, connectionDriver, jdbcUrlDriverId, connectorJarName, Collections.emptySet());
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

    public static DatabaseVendor fromValue(String databaseType) {
        for (DatabaseVendor databaseVendor : values()) {
            if (databaseVendor.databaseType.equals(databaseType)) {
                return databaseVendor;
            }
        }
        throw new UnsupportedOperationException(String.format("%s is not a DatabaseVendor", databaseType));
    }

    public static Optional<DatabaseVendor> getVendorByJdbcUrl(DatabaseV4Request configRequest) {
        Optional<DatabaseRequestParameters> rdsConfigRequestParameters = getParameters(configRequest);
        Optional<String> version = databaseVersion(rdsConfigRequestParameters);
        for (DatabaseVendor databaseVendor : values()) {
            if (configRequest.getConnectionURL().startsWith(String.format("jdbc:%s:", databaseVendor.jdbcUrlDriverId))) {
                if (databaseVendor.versions.isEmpty()) {
                    return Optional.of(databaseVendor);
                } else if (version.isPresent()) {
                    Optional<String> versionMatchFound = databaseVendor.versions.stream().filter(item -> item.equalsIgnoreCase(version.get())).findFirst();
                    if (versionMatchFound.isPresent()) {
                        return Optional.of(databaseVendor);
                    }
                } else {
                    return Optional.of(databaseVendor);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<DatabaseRequestParameters> getParameters(DatabaseV4Request configRequest) {
        if (configRequest.getConnectionURL().contains(String.format("jdbc:%s:", ORACLE11.jdbcUrlDriverId))) {
            return Optional.ofNullable(configRequest.getOracle());
        }
        return Optional.empty();
    }

    private static Optional<String> databaseVersion(Optional<DatabaseRequestParameters> rdsConfigRequestParameters) {
        return rdsConfigRequestParameters.isPresent() ? Optional.of(rdsConfigRequestParameters.get().getVersion()) : Optional.empty();
    }

    public static Collection<DatabaseVendor> availableVendors() {
        return EnumSet.complementOf(EnumSet.of(EMBEDDED));
    }

    public static Collection<DatabaseVendor> outOfTheBoxVendors() {
        return EnumSet.of(POSTGRES, MYSQL, MARIADB);
    }
}
