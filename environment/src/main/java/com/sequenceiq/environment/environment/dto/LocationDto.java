package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LocationDto {

    private final String name;

    private final String displayName;

    private final Double latitude;

    private final Double longitude;

    @JsonCreator
    public LocationDto(
            @JsonProperty("name") String name,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude) {
        this.name = name;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(name) && latitude == null && longitude == null;
    }

    public String getName() {
        return name;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "LocationDto{" +
            "name='" + name + '\'' +
            ", displayName='" + displayName + '\'' +
            ", latitude=" + latitude +
            ", longitude=" + longitude +
            '}';
    }

    public static LocationDtoBuilder builder() {
        return new LocationDtoBuilder();
    }

    public static final class LocationDtoBuilder {
        private String name;

        private String displayName;

        private Double latitude;

        private Double longitude;

        private LocationDtoBuilder() {
        }

        public LocationDtoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public LocationDtoBuilder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public LocationDtoBuilder withLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public LocationDtoBuilder withLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public LocationDto build() {
            return new LocationDto(name, displayName, latitude, longitude);
        }
    }
}
