package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

public enum DatabaseVendor {
    POSTGRES("postgres", "Postgres", "org.postgresql.Driver"),
    MYSQL("mysql", "MySQL", "org.mysql.Driver"),
    MARIADB("mysql", "MySQL", "org.mysql.Driver"),
    MSSQL("mssql", "SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    ORACLE("oracle", "Oracle", "oracle.jdbc.driver.OracleDriver"),
    SQLANYWHERE("sqlanywhere", "SQLAnywhere", "org.postgresql.Driver"),
    EMBEDDED("embedded", "", "");

    private final String value;
    private final String fancyName;
    private final String connectionDriver;

    DatabaseVendor(String value, String fancyName, String connectionDriver) {
        this.value = value;
        this.fancyName = fancyName;
        this.connectionDriver = connectionDriver;
    }

    public final String value() {
        return value;
    }

    public final String fancyName() {
        return fancyName;
    }

    public String connectionDriver() {
        return connectionDriver;
    }

    public static DatabaseVendor fromValue(String value) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.value.equals(value)) {
                return vendor;
            }
        }
        throw new UnsupportedOperationException("Not a DatabaseVendor value");
    }

    public static Optional<DatabaseVendor> getVendorByJdbcUrl(String jdbcUrl) {
        for (DatabaseVendor vendor : values()) {
            if (jdbcUrl.contains(String.format("jdbc:%s", vendor.value))) {
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
