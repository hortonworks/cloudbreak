package com.sequenceiq.environment.api.v1.environment.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LocationV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationRequest implements Serializable {

    @Size(max = 100)
    @Schema(description = EnvironmentModelDescription.LOCATION)
    private @NotNull String name;

    @Schema(description = EnvironmentModelDescription.LATITUDE)
    private Double latitude;

    @Schema(description = EnvironmentModelDescription.LONGITUDE)
    private Double longitude;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "LocationRequest{" +
                "name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
