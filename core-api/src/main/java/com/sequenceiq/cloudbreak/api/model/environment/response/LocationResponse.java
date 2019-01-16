package com.sequenceiq.cloudbreak.api.model.environment.response;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationResponse {

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    private String locationName;

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION_DISPLAY_NAME)
    private String locationDisplayName;

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

    public String getLocationDisplayName() {
        return locationDisplayName;
    }

    public void setLocationDisplayName(String locationDisplayName) {
        this.locationDisplayName = locationDisplayName;
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
