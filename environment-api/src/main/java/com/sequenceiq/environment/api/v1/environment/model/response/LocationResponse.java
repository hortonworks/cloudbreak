package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LocationV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationResponse implements Serializable {

    @Schema(description = EnvironmentModelDescription.LOCATION, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = EnvironmentModelDescription.LOCATION_DISPLAY_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;

    @Schema(description = EnvironmentModelDescription.LATITUDE, requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @Schema(description = EnvironmentModelDescription.LONGITUDE, requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "LocationResponse{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public static final class LocationResponseBuilder {
        private String name;

        private String displayName;

        private Double latitude;

        private Double longitude;

        private LocationResponseBuilder() {
        }

        public static LocationResponseBuilder aLocationResponse() {
            return new LocationResponseBuilder();
        }

        public LocationResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public LocationResponseBuilder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public LocationResponseBuilder withLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public LocationResponseBuilder withLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public LocationResponse build() {
            LocationResponse locationResponse = new LocationResponse();
            locationResponse.setName(name);
            locationResponse.setDisplayName(displayName);
            locationResponse.setLatitude(latitude);
            locationResponse.setLongitude(longitude);
            return locationResponse;
        }
    }
}
