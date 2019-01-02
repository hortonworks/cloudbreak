package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationV4Response {

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    private String name;

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION_DISPLAY_NAME)
    private String displayName;

    @ApiModelProperty(EnvironmentRequestModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(EnvironmentRequestModelDescription.LONGITUDE)
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
}
