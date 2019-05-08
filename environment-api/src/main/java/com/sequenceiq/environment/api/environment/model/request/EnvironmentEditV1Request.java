package com.sequenceiq.environment.api.environment.model.request;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Size;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EnvironmentEditV1Request {

    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.LOCATION)
    private LocationV1Request location;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions;
    }

    public LocationV1Request getLocation() {
        return location;
    }

    public void setLocation(LocationV1Request location) {
        this.location = location;
    }
}
