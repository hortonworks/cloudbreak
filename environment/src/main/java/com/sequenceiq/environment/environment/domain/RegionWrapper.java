package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RegionWrapper implements Serializable {

    private String name;

    private String displayName;

    private Double latitude;

    private Double longitude;

    private Set<String> regions;

    public RegionWrapper(String name, String displayName, Double latitude,
            Double longitude, Set<String> regions) {
        this.name = name;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.regions = regions;
    }

    public RegionWrapper() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Set<String> getRegions() {
        return new HashSet<>(regions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegionWrapper that = (RegionWrapper) o;
        return Objects.equals(name, that.name)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(latitude, that.latitude)
                && Objects.equals(longitude, that.longitude)
                && Objects.equals(regions, that.regions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, latitude, longitude, regions);
    }

    @Override
    public String toString() {
        return "RequestedLocation{"
                + "name='" + name + '\''
                + ", displayName='" + displayName + '\''
                + ", latitude=" + latitude
                + ", longitude=" + longitude
                + ", regions=" + regions
                + '}';
    }
}
