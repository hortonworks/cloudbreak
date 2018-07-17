package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PackageVersionsResponse {
    @JsonProperty("return")
    private List<Map<String, Map<String, String>>> result;

    public List<Map<String, Map<String, String>>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Map<String, String>>> result) {
        this.result = result;
    }
}
