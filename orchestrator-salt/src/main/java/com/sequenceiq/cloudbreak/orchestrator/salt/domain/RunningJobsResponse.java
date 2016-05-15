package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunningJobsResponse {

    @JsonProperty("return")
    private List<Map<String, Map<String, Object>>> result;

    public List<Map<String, Map<String, Object>>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Map<String, Object>>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RunningJobsResponse{"
                + "result=" + result
                + '}';
    }
}
