package com.sequenceiq.environment.api.v1.environment.model.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "LocationV1Request")
public class LocationRequest {

    @Size(max = 100)
    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private @NotNull String name;

    @ApiModelProperty(EnvironmentModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(EnvironmentModelDescription.LONGITUDE)
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
}
