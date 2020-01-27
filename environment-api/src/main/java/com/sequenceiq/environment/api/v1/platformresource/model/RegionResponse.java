package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionResponse extends CompactRegionResponse {
    @ApiModelProperty(PlatformResourceModelDescription.AVAILABILITY_ZONES)
    private  Map<String, List<String>> availabilityZones;

    @ApiModelProperty(PlatformResourceModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    @ApiModelProperty(PlatformResourceModelDescription.REGION_LOCATIONS)
    private List<String> locations;

    @ApiModelProperty(PlatformResourceModelDescription.K8S_SUPPORTED_LOCATIONS)
    private List<String> k8sSupportedlocations;

    public RegionResponse() {
        availabilityZones = new HashMap<>();
        locations = new ArrayList<>();
        k8sSupportedlocations = new ArrayList<>();
    }

    @JsonProperty("names")
    public List<String> getNames() {
        return super.getNames();
    }

    @JsonProperty("names")
    public void setNames(List<String> names) {
        super.setNames(names);
    }

    public Map<String, List<String>> getAvailabilityZones() {
        return availabilityZones;
    }

    public void setAvailabilityZones(Map<String, List<String>> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public List<String> getK8sSupportedlocations() {
        return k8sSupportedlocations;
    }

    public void setK8sSupportedlocations(List<String> k8sSupportedlocations) {
        this.k8sSupportedlocations = k8sSupportedlocations;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegions) {
        defaultRegion = defaultRegions;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }
}
