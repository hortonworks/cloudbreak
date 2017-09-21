package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionsResponse implements JsonEntity {

    private Map<String, RegionResponse> regionsResponse = new HashMap<>();

    public Map<String, RegionResponse> getRegionsResponse() {
        return regionsResponse;
    }

    public void setRegionsResponse(Map<String, RegionResponse> regionsResponse) {
        this.regionsResponse = regionsResponse;
    }
}
