package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompactRegionV4Response implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.REGIONS)
    private Set<String> regions;

    @ApiModelProperty(ConnectorModelDescription.REGION_DISPLAYNAMES)
    private Map<String, String> displayNames;

    public CompactRegionV4Response() {
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
