package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandExecutionResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, String>> result;

    public List<Map<String, String>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, String>> result) {
        this.result = result;
    }

    public Map<String, String> getResultByMinionId() {
        Map<String, String> res = new HashMap<>();
        result.forEach(res::putAll);
        return res;
    }
}
