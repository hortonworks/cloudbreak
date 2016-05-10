package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PingResponse {

    @JsonProperty("return")
    private List<Map<String, Boolean>> result;

    public List<Map<String, Boolean>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Boolean>> result) {
        this.result = result;
    }
}
