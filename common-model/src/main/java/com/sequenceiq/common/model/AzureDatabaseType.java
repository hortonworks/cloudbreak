package com.sequenceiq.common.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureDatabaseType implements DatabaseType {

    SINGLE_SERVER("postgresqlServer", false, "servers"),

    FLEXIBLE_SERVER("flexiblePostgresqlServer", true, "flexibleServers");

    public static final String AZURE_DATABASE_TYPE_KEY = "AZURE_DATABASE_TYPE";

    public static final String AZURE_AUTOMIGRATION_ERROR_PREFIX = "Automigration happened from Single to Flexible Server";

    private final String shortName;

    private final boolean databasePauseSupported;

    private final String referenceType;

    AzureDatabaseType(String shortName, boolean databasePauseSupported, String referenceType) {
        this.shortName = shortName;
        this.databasePauseSupported = databasePauseSupported;
        this.referenceType = referenceType;
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

    @Override
    public String referenceType() {
        return referenceType;
    }

    public static AzureDatabaseType safeValueOf(String databaseTypeString) {
        try {
            return StringUtils.isNotBlank(databaseTypeString) ? valueOf(databaseTypeString) : SINGLE_SERVER;
        } catch (IllegalArgumentException ex) {
            return SINGLE_SERVER;
        }
    }
}
