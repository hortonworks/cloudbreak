package com.sequenceiq.environment.api.environment.v1.model.request;

import javax.validation.constraints.NotNull;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationRequest {

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
