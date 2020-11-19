package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PingResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, Boolean>> result;

    public List<Map<String, Boolean>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Boolean>> result) {
        this.result = result;
    }

    public Map<String, Boolean> getResultByMinionId() {
        Map<String, Boolean> res = new HashMap<>();
        result.forEach(map -> map.forEach(res::put));
        return res;
    }
}
