package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionResponse implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.REGIONS)
    private Set<String> regions;

    @ApiModelProperty(ConnectorModelDescription.REGION_DISPLAYNAMES)
    private Map<String, String> displayNames;

    @ApiModelProperty(ConnectorModelDescription.AVAILABILITY_ZONES)
    private  Map<String, Collection<String>> availabilityZones;

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    public RegionResponse() {
        regions = new HashSet<>();
        availabilityZones = new HashMap<>();
        displayNames = new HashMap<>();
    }

    public Set<String> getRegions() {
        return regions;
    }

    public void setRegions(Set<String> regions) {
        this.regions = regions;
    }

    public Map<String, String> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, String> displayNames) {
        this.displayNames = displayNames;
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
