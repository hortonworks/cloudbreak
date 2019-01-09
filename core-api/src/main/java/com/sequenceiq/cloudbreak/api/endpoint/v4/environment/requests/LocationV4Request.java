package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationV4Request {

    @ApiModelProperty(ModelDescriptions.EnvironmentRequestModelDescription.LOCATION)
    @NotNull
    private String locationName;

    @ApiModelProperty(ModelDescriptions.EnvironmentRequestModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(ModelDescriptions.EnvironmentRequestModelDescription.LONGITUDE)
    private Double longitude;

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
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
}
