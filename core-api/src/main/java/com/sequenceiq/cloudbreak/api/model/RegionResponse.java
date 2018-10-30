package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionResponse extends CompactRegionResponse {
    @ApiModelProperty(ConnectorModelDescription.AVAILABILITY_ZONES)
    private  Map<String, Collection<String>> availabilityZones;

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    public RegionResponse() {
        availabilityZones = new HashMap<>();
    }

    @JsonProperty("regions")
    public Set<String> getRegions() {
        return super.getRegions();
    }

    @JsonProperty("regions")
    public void setRegions(Set<String> regions) {
        super.setRegions(regions);
    }

    public Map<String, Collection<String>> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Map<String, Collection<String>> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegions) {
        defaultRegion = defaultRegions;
    }
}
