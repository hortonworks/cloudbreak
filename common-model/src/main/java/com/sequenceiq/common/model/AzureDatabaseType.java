package com.sequenceiq.common.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureDatabaseType implements DatabaseType {

    SINGLE_SERVER("postgresqlServer"),

    FLEXIBLE_SERVER("flexiblePostgresqlServer");

    public static final String AZURE_DATABASE_TYPE_KEY = "AZURE_DATABASE_TYPE";

    private final String shortName;

    AzureDatabaseType(String shortName) {
        this.shortName = shortName;
    }

    public boolean isSingleServer() {
        return this == SINGLE_SERVER;
    }

    public String shortName() {
        return shortName;
    }

    public static AzureDatabaseType safeValueOf(String databaseTypeString) {
        try {
            return StringUtils.isNotBlank(databaseTypeString) ? valueOf(databaseTypeString) : SINGLE_SERVER;
        } catch (IllegalArgumentException ex) {
            return SINGLE_SERVER;
        }
    }
}
