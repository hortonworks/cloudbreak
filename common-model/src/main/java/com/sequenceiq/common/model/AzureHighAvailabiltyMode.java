package com.sequenceiq.common.model;

import org.apache.commons.lang3.StringUtils;

public enum AzureHighAvailabiltyMode {
    DISABLED("Disabled"),

    SAME_ZONE("SameZone"),

    ZONE_REDUNDANT("ZoneRedundant");

    public static final String AZURE_HA_MODE_KEY = "AZURE_HA_MODE";

    private final String templateValue;

    AzureHighAvailabiltyMode(String templateValue) {
        this.templateValue = templateValue;
    }

    public String templateValue() {
        return templateValue;
    }

    public static AzureHighAvailabiltyMode safeValueOf(String highAvailabilityMode) {
        try {
            return StringUtils.isNotBlank(highAvailabilityMode) ? valueOf(highAvailabilityMode) : DISABLED;
        } catch (IllegalArgumentException ex) {
            return DISABLED;
        }
    }
}
