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

    public Map<String, String> getResult() {
        Map<String, String> res = new HashMap<>();
        result.stream().forEach(map -> map.forEach((k, v) -> res.put(v, k)));
        return res;
    }

}
