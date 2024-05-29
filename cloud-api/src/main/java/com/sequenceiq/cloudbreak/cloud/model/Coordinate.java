package com.sequenceiq.cloudbreak.cloud.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Coordinate {

    private final String displayName;

    private final Double longitude;

    private final Double latitude;

    private final String key;

    private final boolean k8sSupported;

    private final String defaultDbVmType;

    private final List<String> entitlements;

    protected Coordinate(Double longitude, Double latitude, String displayName, String key, boolean k8sSupported,
        List<String> entitlements, String defaultDbVmType) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.displayName = displayName;
        this.key = key;
        this.k8sSupported = k8sSupported;
        this.entitlements = entitlements;
        this.defaultDbVmType = defaultDbVmType;
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

    public boolean isK8sSupported() {
        return k8sSupported;
    }

    public String getDefaultDbVmType() {
        return defaultDbVmType;
    }

    public boolean isMatchedRegion(Region region) {
        return Objects.equals(key, region.getRegionName()) || Objects.equals(displayName, region.getRegionName());
    }

    public static Coordinate coordinate(String longitude, String latitude, String  displayName, String key, boolean k8sSupported,
        List<String> entitlements, String defaultDbVmType) {
        return new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude), displayName, key, k8sSupported, entitlements,
                defaultDbVmType);
    }

    public static Coordinate defaultCoordinate() {
        return new Coordinate(
                Double.parseDouble("36.7477169"),
                Double.parseDouble("-119.7729841"),
                "California (West US)",
                "us-west-1",
                false,
                new ArrayList<>(),
                null);
    }
}
