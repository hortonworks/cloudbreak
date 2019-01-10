package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegionsResponse implements JsonEntity {

    private Map<String, RegionV4Response> regionsResponse = new HashMap<>();

    public Map<String, RegionV4Response> getRegionsResponse() {
        return regionsResponse;
    }

    public void setRegionsResponse(Map<String, RegionV4Response> regionsResponse) {
        this.regionsResponse = regionsResponse;
    }
}
