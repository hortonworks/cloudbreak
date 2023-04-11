package com.sequenceiq.environment.environment.dto;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = LocationDto.Builder.class)
public class LocationDto {

    private final String name;

    private final String displayName;

    private final Double latitude;

    private final Double longitude;

    public LocationDto(Builder builder) {
        this.name = builder.name;
        this.displayName = builder.displayName;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
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

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder
    public static final class Builder {
        private String name;

        private String displayName;

        private Double latitude;

        private Double longitude;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder withLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public LocationDto build() {
            return new LocationDto(this);
        }
    }
}
