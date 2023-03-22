package com.sequenceiq.sdx.api.model;

public enum SdxDatabaseAvailabilityType {
    NONE,
    NON_HA,
    HA;

    public static boolean hasExternalDatabase(SdxDatabaseAvailabilityType availabilityType) {
        return availabilityType != NONE;
    }
}
