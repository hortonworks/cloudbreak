package com.sequenceiq.cloudbreak.api.model.environment.request;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationRequest {

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    private @NotNull String locationName;

    @ApiModelProperty(EnvironmentRequestModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(EnvironmentRequestModelDescription.LONGITUDE)
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
