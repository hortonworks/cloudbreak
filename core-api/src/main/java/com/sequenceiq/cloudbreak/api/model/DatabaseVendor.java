package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

public enum DatabaseVendor {
    POSTGRES("postgres", "Postgres", "org.postgresql.Driver", "postgresql"),
    MYSQL("mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql"),
    MARIADB("mysql", "MySQL", "com.mysql.jdbc.Driver", "mysql"),
    MSSQL("mssql", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sqlserver"),
    ORACLE("oracle", "Oracle", "oracle.jdbc.driver.OracleDriver", "oracle"),
    SQLANYWHERE("sqlanywhere", "SQLAnywhere", "org.postgresql.Driver", "sqlanywhere"),
    EMBEDDED("embedded", "", "", "");

    private final String ambariVendor;
    private final String fancyName;
    private final String connectionDriver;
    private final String jdbcUrlDriverId;

    DatabaseVendor(String ambariVendor, String fancyName, String connectionDriver, String jdbcUrlDriverId) {
        this.ambariVendor = ambariVendor;
        this.fancyName = fancyName;
        this.connectionDriver = connectionDriver;
        this.jdbcUrlDriverId = jdbcUrlDriverId;
    }

    public final String ambariVendor() {
        return ambariVendor;
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

    public static DatabaseVendor fromValue(String ambariVendor) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.ambariVendor.equals(ambariVendor)) {
                return vendor;
            }
        }
        throw new UnsupportedOperationException("Not a DatabaseVendor ambariVendor");
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
