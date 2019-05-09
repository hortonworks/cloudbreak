package com.sequenceiq.environment.api.environment.model.response;

import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationV1Response {

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
}
