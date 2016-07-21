package com.sequenceiq.cloudbreak.api.model;

public enum DatabaseVendor {
    POSTGRES("postgres", "Postgres"),
    MYSQL("mysql", "MySQL"),
    MARIADB("mysql", "MySQL"),
//    MSSQL("mssql", "SQLServer"),
//    ORACLE("oracle", "Oracle"),
//    SQLANYWHERE("sqlanywhere", "SQLAnywhere"),
    EMBEDDED("embedded", "");

    private final String value;
    private final String fancyName;

    DatabaseVendor(String value, String fancyName) {
        this.value = value;
        this.fancyName = fancyName;
    }

    public final String value() {
        return value;
    }

    public final String fancyName() {
        return fancyName;
    }

    public static DatabaseVendor fromValue(String value) {
        for (DatabaseVendor vendor : values()) {
            if (vendor.value.equals(value)) {
                return vendor;
            }
        }
        throw new UnsupportedOperationException("Not a DatabaseVendor value");
    }
}
