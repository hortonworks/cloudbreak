package com.sequenceiq.cloudbreak.service.stack.connector.gcc.domain;

public enum GccZone {

    US_CENTRAL1_A("us-central1-a"),
    US_CENTRAL1_B("us-central1-b"),
    US_CENTRAL1_F("us-central1-f"),
    EUROPE_WEST1_B("europe-west1-b"),
    ASIA_EAST1_A("asia-east1-a"),
    ASIA_EAST1_B("asia-east1-b");

    private final String value;

    private GccZone(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GccZone fromName(String regionName) {
        for (GccZone region : GccZone.values()) {
            if (regionName.equals(region.getValue())) {
                return region;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + regionName + " value!");
    }
}
