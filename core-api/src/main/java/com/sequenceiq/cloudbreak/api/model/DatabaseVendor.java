package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

public enum DatabaseVendor {
    POSTGRES("postgres", "postgres", "Postgres", "org.postgresql.Driver", "postgresql", ""),
    MYSQL("mysql", "mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MARIADB("mysql", "mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql", "mysql-connector-java.jar"),
    MSSQL("mssql", "mssql", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver", ""),
    ORACLE11("oracle", "oracle", "Oracle 11g", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc6.jar"),
    ORACLE12("oracle", "oracle", "Oracle 12c", "oracle.jdbc.driver.OracleDriver", "oracle", "ojdbc7.jar"),
    SQLANYWHERE("sqlanywhere", "sqlanywhere", "SQLAnywhere", "org.postgresql.Driver", "sqlanywhere", ""),
    EMBEDDED("embedded", "embedded", "", "", "", "");

    private final String ambariVendor;
    private final String fancyName;
    private final String connectionDriver;
    private final String jdbcUrlDriverId;
    private final String connectorJarName;
    private final String databaseType;

    DatabaseVendor(String ambariVendor, String databaseType, String fancyName, String connectionDriver, String jdbcUrlDriverId, String connectorJarName) {
        this.ambariVendor = ambariVendor;
        this.databaseType = databaseType;
        this.fancyName = fancyName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
        this.connectorJarName = connectorJarName;
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

    public static DatabaseVendor fromValue(String ambariVendor) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.ambariVendor.equals(ambariVendor)) {
                return vendor;
            }
        }
        throw new UnsupportedOperationException(String.format("%s is not a DatabaseVendor", ambariVendor));
    }

    public static Optional<DatabaseVendor> getVendorByJdbcUrl(String jdbcUrl) {
        for (DatabaseVendor vendor : values()) {
            if (jdbcUrl.startsWith(String.format("jdbc:%s:", vendor.jdbcUrlDriverId))) {
                return Optional.of(vendor);
            }
        }
        return Optional.empty();
    }

    public static Collection<DatabaseVendor> availableVendors() {
        return EnumSet.complementOf(EnumSet.of(EMBEDDED));
    }

    public static Collection<DatabaseVendor> outOfTheBoxVendors() {
        return EnumSet.of(POSTGRES, MYSQL, MARIADB);
    }
}
