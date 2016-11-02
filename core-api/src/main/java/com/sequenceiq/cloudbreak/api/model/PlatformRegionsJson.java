package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformRegionsJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.REGIONS)
    private Map<String, Collection<String>> regions;

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.AVAILABILITY_ZONES)
    private Map<String, Map<String, Collection<String>>> availabilityZones;

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DEFAULT_REGIOS)
    private Map<String, String> defaultRegions;

    public PlatformRegionsJson() {
        this.regions = new HashMap<>();
        this.availabilityZones = new HashMap<>();
        this.defaultRegions = new HashMap<>();
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
}
