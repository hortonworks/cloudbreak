package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;

public enum DatabaseVendor {
    POSTGRES("postgres", "postgres", "Postgres", "org.postgresql.Driver", "postgresql", ""),
    MYSQL("mysql", "mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MARIADB("mysql", "mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MSSQL("mssql", "mssql", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver", ""),
    ORACLE11("oracle", "oracle", "Oracle 11g", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc6.jar",
            Sets.newHashSet("oracle11g", "oracle11", "11", "11g")),
    ORACLE12("oracle", "oracle", "Oracle 12c", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc7.jar",
            Sets.newHashSet("oracle12c", "oracle12", "12", "12c")),
    SQLANYWHERE("sqlanywhere", "sqlanywhere", "SQLAnywhere", "org.postgresql.Driver", "sqlanywhere", ""),
    EMBEDDED("embedded", "embedded", "", "", "", "");

    private final String ambariVendor;
    private final String fancyName;
    private final String connectionDriver;
    private final String jdbcUrlDriverId;
    private final String connectorJarName;
    private final String databaseType;
    private final Set<String> versions;

    DatabaseVendor(String ambariVendor, String databaseType, String fancyName, String connectionDriver, String jdbcUrlDriverId, String connectorJarName,
            Set<String> versions) {
        this.ambariVendor = ambariVendor;
        this.databaseType = databaseType;
        this.fancyName = fancyName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
        this.connectorJarName = connectorJarName;
        this.versions = versions;
    }

    DatabaseVendor(String ambariVendor, String databaseType, String fancyName, String connectionDriver, String jdbcUrlDriverId, String connectorJarName) {
        this.ambariVendor = ambariVendor;
        this.databaseType = databaseType;
        this.fancyName = fancyName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
        this.connectorJarName = connectorJarName;
        this.versions = Sets.newHashSet();
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
        Map<String, Object> parameters = getParameters(configRequest);
        Optional<String> version = databaseVersion(parameters);
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

    private static Map<String, Object> getParameters(RDSConfigRequest configRequest) {
        Map<String, Object> result = new HashMap<>();
        if (configRequest.getConnectionURL().contains(String.format("jdbc:%s:", ORACLE11.jdbcUrlDriverId))) {
            result = configRequest.getOracleProperties();
        }
        return result;
    }

    private static Optional<String> databaseVersion(Map<String, Object> parameters) {
        if (parameters.entrySet().isEmpty() || !parameters.containsKey("version")) {
            return Optional.empty();
        } else {
            return Optional.of(parameters.get("version").toString());
        }
    }

    public static Collection<DatabaseVendor> availableVendors() {
        return EnumSet.complementOf(EnumSet.of(EMBEDDED));
    }

    public static Collection<DatabaseVendor> outOfTheBoxVendors() {
        return EnumSet.of(POSTGRES, MYSQL, MARIADB);
    }
}
