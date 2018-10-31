package com.sequenceiq.cloudbreak.cloud.model;

public class Coordinate {

    private final String displayName;

    private final Double longitude;

    private final Double latitude;

    private Coordinate(Double longitude, Double latitude, String displayName) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.displayName = displayName;
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

    public static Coordinate coordinate(String longitude, String latitude, String  displayName) {
        return new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude), displayName);
    }

    public static Coordinate defaultCoordinate() {
        return new Coordinate(Double.parseDouble("36.7477169"), Double.parseDouble("-119.7729841"), "California (West US)");
    }
}
