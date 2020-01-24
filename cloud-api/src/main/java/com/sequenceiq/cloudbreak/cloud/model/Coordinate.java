package com.sequenceiq.cloudbreak.cloud.model;

public class Coordinate {

    private final String displayName;

    private final Double longitude;

    private final Double latitude;

    private final String key;

    private final boolean k8sSupported;

    private Coordinate(Double longitude, Double latitude, String displayName, String key, boolean k8sSupported) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.displayName = displayName;
        this.key = key;
        this.k8sSupported = k8sSupported;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean getK8sSupported() {
        return k8sSupported;
    }

    public String getKey() {
        return key;
    }

    public static Coordinate coordinate(String longitude, String latitude, String  displayName, String key, boolean k8sSupported) {
        return new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude), displayName, key, k8sSupported);
    }

    public static Coordinate defaultCoordinate() {
        return new Coordinate(
                Double.parseDouble("36.7477169"),
                Double.parseDouble("-119.7729841"),
                "California (West US)",
                "us-west-1",
                false);
    }
}
