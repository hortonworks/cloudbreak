package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;

import io.swagger.annotations.ApiModel;

@ApiModel
@NotNull
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionAwareImageLocationV4Response implements JsonEntity {

    @JsonProperty("region")
    private String region;

    @JsonProperty("location")
    private String location;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
