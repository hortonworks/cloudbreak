package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;

public class Coordinate {

    private final String displayName;

    private final Double longitude;

    private final Double latitude;

    private final String key;

    private final boolean k8sSupported;

    private final List<String> entitlements;

    private Coordinate(Double longitude, Double latitude, String displayName, String key, boolean k8sSupported, List<String> entitlements) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.displayName = displayName;
        this.key = key;
        this.k8sSupported = k8sSupported;
        this.entitlements = entitlements;
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

    public List<String> getEntitlements() {
        return entitlements;
    }

    public static Coordinate coordinate(String longitude, String latitude, String  displayName, String key, boolean k8sSupported, List<String> entitlements) {
        return new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude), displayName, key, k8sSupported, entitlements);
    }

    public static Coordinate defaultCoordinate() {
        return new Coordinate(
                Double.parseDouble("36.7477169"),
                Double.parseDouble("-119.7729841"),
                "California (West US)",
                "us-west-1",
                false,
                new ArrayList<>());
    }
}
