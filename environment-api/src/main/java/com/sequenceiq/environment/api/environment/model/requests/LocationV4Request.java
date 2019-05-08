package com.sequenceiq.environment.api.environment.model.requests;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.environment.api.environment.doc.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class LocationV4Request {

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    private @NotNull String name;

    @ApiModelProperty(EnvironmentRequestModelDescription.LATITUDE)
    private Double latitude;

    @ApiModelProperty(EnvironmentRequestModelDescription.LONGITUDE)
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

    public boolean isEmpty() {
        return StringUtils.isEmpty(name) && latitude == null && longitude == null;
    }
}
