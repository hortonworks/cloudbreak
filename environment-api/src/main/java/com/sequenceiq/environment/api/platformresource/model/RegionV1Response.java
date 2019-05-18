package com.sequenceiq.environment.api.platformresource.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.environment.model.response.CompactRegionV1Response;
import com.sequenceiq.environment.api.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionV1Response extends CompactRegionV1Response {
    @ApiModelProperty(PlatformResourceModelDescription.AVAILABILITY_ZONES)
    private  Map<String, Collection<String>> availabilityZones;

    @ApiModelProperty(PlatformResourceModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    @ApiModelProperty(PlatformResourceModelDescription.REGION_LOCATIONS)
    private Set<String> locations;

    public RegionV1Response() {
        availabilityZones = new HashMap<>();
        locations = new HashSet<>();
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

    public Set<String> getLocations() {
        return locations;
    }

    public void setLocations(Set<String> locations) {
        this.locations = locations;
    }
}
