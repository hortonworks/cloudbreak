package com.sequenceiq.common.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureDatabaseType {
    SINGLE_SERVER,

    FLEXIBLE_SERVER;

    public static final String AZURE_DATABASE_TYPE_KEY = "AZURE_DATABASE_TYPE";

    public boolean isSingleServer() {
        return this == SINGLE_SERVER;
    }

    public static AzureDatabaseType safeValueOf(String databaseTypeString) {
        try {
            return StringUtils.isNotBlank(databaseTypeString) ? valueOf(databaseTypeString) : SINGLE_SERVER;
        } catch (IllegalArgumentException ex) {
            return SINGLE_SERVER;
        }
    }
}
