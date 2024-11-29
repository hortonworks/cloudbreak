package com.sequenceiq.environment.api.v1.platformresource.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CompactRegionV1Response")
public class RegionResponse extends CompactRegionResponse {
    @Schema(description = PlatformResourceModelDescription.AVAILABILITY_ZONES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, List<String>> availabilityZones;

    @Schema(description = PlatformResourceModelDescription.DEFAULT_REGIOS)
    private String defaultRegion;

    @Schema(description = PlatformResourceModelDescription.REGION_LOCATIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> locations;

    @Schema(description = PlatformResourceModelDescription.K8S_SUPPORTED_LOCATIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> k8sSupportedlocations;

    @Schema(description = PlatformResourceModelDescription.CDP_SERVICES, requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Set<String>> cdpSupportedServices;

    public RegionResponse() {
        availabilityZones = new HashMap<>();
        locations = new ArrayList<>();
        k8sSupportedlocations = new ArrayList<>();
        cdpSupportedServices = new HashMap<>();
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

    public Map<String, Set<String>> getCdpSupportedServices() {
        return cdpSupportedServices;
    }

    public void setCdpSupportedServices(Map<String, Set<String>> cdpSupportedServices) {
        this.cdpSupportedServices = cdpSupportedServices;
    }

    @Override
    public String toString() {
        return "RegionResponse{" +
                "availabilityZones=" + availabilityZones +
                ", defaultRegion='" + defaultRegion + '\'' +
                ", locations=" + locations +
                ", cdpSupportedServices=" + cdpSupportedServices +
                '}';
    }
}
