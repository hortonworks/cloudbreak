package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultRouteResponse {

    @JsonProperty("return")
    private List<Map<String, List<Map<String, String>>>> result;

    public DefaultRouteResponse() {
    }

    public DefaultRouteResponse(List<Map<String, List<Map<String, String>>>> result) {
        this.result = result;
    }

    public void setResult(List<Map<String, List<Map<String, String>>>> result) {
        this.result = result;
    }

    public String getGatewayInterfaceName() {
        return result.stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()).stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()).stream()
                    .filter(i -> "UG".equals(i.get("flags")))
                    .limit(1L)
                    .map(i -> i.get("interface"))
                        .findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "DefaultRouteResponse{result=" + result + '}';
    }
}