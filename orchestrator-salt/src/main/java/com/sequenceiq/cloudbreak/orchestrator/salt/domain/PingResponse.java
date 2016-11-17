package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public Map<String, Boolean> getResultByMinionId() {
        Map<String, Boolean> res = new HashMap<>();
        result.stream().forEach(map -> map.forEach((k, v) -> res.put(k, v)));
        return res;
    }
}
