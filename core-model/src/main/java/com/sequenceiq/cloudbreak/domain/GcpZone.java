package com.sequenceiq.cloudbreak.domain;

public enum GcpZone {

    US_CENTRAL1_A("us-central1-a", "us-central1"),
    US_CENTRAL1_B("us-central1-b", "us-central1"),
    US_CENTRAL1_F("us-central1-f", "us-central1"),
    US_CENTRAL1_C("us-central1-c", "us-central1"),
    EUROPE_WEST1_B("europe-west1-b", "europe-west1"),
    EUROPE_WEST1_C("europe-west1-c", "europe-west1"),
    EUROPE_WEST1_D("europe-west1-d", "europe-west1"),
    ASIA_EAST1_A("asia-east1-a", "asia-east1"),
    ASIA_EAST1_B("asia-east1-b", "asia-east1"),
    ASIA_EAST1_C("asia-east1-c", "asia-east1");

    private final String value;
    private final String region;

    private GcpZone(String value, String region) {
        this.value = value;
        this.region = region;
    }

    public String getValue() {
        return value;
    }

    public String getRegion() {
        return region;
    }

    public static GcpZone fromName(String regionName) {
        for (GcpZone region : GcpZone.values()) {
            if (regionName.equals(region.getValue())) {
                return region;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + regionName + " value!");
    }
}
