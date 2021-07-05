package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "LocationV1Response")
public class LocationResponse implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private String name;

    @ApiModelProperty(EnvironmentModelDescription.LOCATION_DISPLAY_NAME)
    private String displayName;

    @ApiModelProperty(EnvironmentModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(EnvironmentModelDescription.LONGITUDE)
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
