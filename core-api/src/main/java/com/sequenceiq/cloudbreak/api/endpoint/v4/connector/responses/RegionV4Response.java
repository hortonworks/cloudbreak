package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionV4Response extends CompactRegionV4Response {
    @Schema(description = ConnectorModelDescription.AVAILABILITY_ZONES)
    private  Map<String, Collection<String>> availabilityZones;

    @Schema(description = ConnectorModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    @Schema(description = ConnectorModelDescription.REGION_LOCATIONS)
    private Set<String> locations;

    public RegionV4Response() {
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
