package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRegionsJson implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.REGIONS)
    private Map<String, Collection<String>> regions;

    @ApiModelProperty(ConnectorModelDescription.REGION_DISPLAYNAMES)
    private Map<String, Map<String, String>> displayNames;

    @ApiModelProperty(ConnectorModelDescription.AVAILABILITY_ZONES)
    private Map<String, Map<String, Collection<String>>> availabilityZones;

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_REGIOS)
    private Map<String, String> defaultRegions;

    public PlatformRegionsJson() {
        regions = new HashMap<>();
        availabilityZones = new HashMap<>();
        defaultRegions = new HashMap<>();
        displayNames = new HashMap<>();
    }

    public Map<String, Collection<String>> getRegions() {
        return regions;
    }

    public void setRegions(Map<String, Collection<String>> regions) {
        this.regions = regions;
    }

    public Map<String, Map<String, Collection<String>>> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Map<String, Map<String, Collection<String>>> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Map<String, String> getDefaultRegions() {
        return defaultRegions;
    }

    public void setDefaultRegions(Map<String, String> defaultRegions) {
        this.defaultRegions = defaultRegions;
    }

    public Map<String, Map<String, String>> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, Map<String, String>> displayNames) {
        this.displayNames = displayNames;
    }
}
