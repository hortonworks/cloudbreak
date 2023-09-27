package com.sequenceiq.common.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureDatabaseType implements DatabaseType {

    SINGLE_SERVER("postgresqlServer", false),

    FLEXIBLE_SERVER("flexiblePostgresqlServer", true);

    public static final String AZURE_DATABASE_TYPE_KEY = "AZURE_DATABASE_TYPE";

    private final String shortName;

    private final boolean databasePauseSupported;

    AzureDatabaseType(String shortName, boolean databasePauseSupported) {
        this.shortName = shortName;
        this.databasePauseSupported = databasePauseSupported;
    }

    public boolean isSingleServer() {
        return this == SINGLE_SERVER;
    }

    @Override
    public String shortName() {
        return shortName;
    }

    @Override
    public boolean isDatabasePauseSupported() {
        return databasePauseSupported;
    }

    public static AzureDatabaseType safeValueOf(String databaseTypeString) {
        try {
            return StringUtils.isNotBlank(databaseTypeString) ? valueOf(databaseTypeString) : SINGLE_SERVER;
        } catch (IllegalArgumentException ex) {
            return SINGLE_SERVER;
        }
    }
}
