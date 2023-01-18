package com.sequenceiq.environment.api.v1.environment.model.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CompactRegionV1Response", subTypes = RegionResponse.class)
public class CompactRegionResponse implements Serializable {

    @Schema(description = EnvironmentModelDescription.REGIONS)
    private List<String> names;

    @Schema(description = EnvironmentModelDescription.REGION_DISPLAYNAMES)
    private Map<String, String> displayNames;

    public CompactRegionResponse() {
        names = new ArrayList<>();
        displayNames = new HashMap<>();
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public Map<String, String> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, String> displayNames) {
        this.displayNames = displayNames;
    }

    @Override
    public String toString() {
        return "CompactRegionResponse{" +
                "names=" + names +
                ", displayNames=" + displayNames +
                '}';
    }
}
