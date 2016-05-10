package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkInterfaceResponse {

    @JsonProperty("return")
    private List<Map<String, String>> result;

    public Map<String, String> getResultGroupByHost() {
        Map<String, String> res = new HashMap<>();
        result.stream().forEach(map -> map.forEach(res::put));
        return res;
    }

    public Map<String, String> getResultGroupByIP() {
        Map<String, String> res = new HashMap<>();
        result.stream().forEach(map -> map.forEach((k, v) -> res.put(v, k)));
        return res;
    }

    public void setResult(List<Map<String, String>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NetworkInterfaceResponse{");
        sb.append("result=").append(result);
        sb.append('}');
        return sb.toString();
    }
}
