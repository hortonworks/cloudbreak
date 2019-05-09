package com.sequenceiq.environment.api.environment.model.response;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompactRegionV1Response implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.REGIONS)
    private Set<String> regions;

    @ApiModelProperty(EnvironmentModelDescription.REGION_DISPLAYNAMES)
    private Map<String, String> displayNames;

    public CompactRegionV1Response() {
        regions = new HashSet<>();
        displayNames = new HashMap<>();
    }

    @JsonProperty("values")
    public Set<String> getRegions() {
        return regions;
    }

    @JsonProperty("values")
    public void setRegions(Set<String> regions) {
        this.regions = regions;
    }

    public Map<String, String> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, String> displayNames) {
        this.displayNames = displayNames;
    }
}
