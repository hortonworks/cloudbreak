package com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Size;

import org.springframework.lang.Nullable;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.EnvironmentRequestModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class EnvironmentEditV4Request {

    @Nullable
    @Size(max = 1000)
    @ApiModelProperty(ModelDescriptions.DESCRIPTION)
    private String description;

    @ApiModelProperty(EnvironmentRequestModelDescription.REGIONS)
    private Set<String> regions = new HashSet<>();

    @ApiModelProperty(EnvironmentRequestModelDescription.LOCATION)
    private LocationV4Request location;

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

    public LocationV4Request getLocation() {
        return location;
    }

    public void setLocation(LocationV4Request location) {
        this.location = location;
    }
}
