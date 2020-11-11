package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Streams;

public class MinionIpAddressesResponse {

    @JsonProperty("return")
    private List<Map<String, JsonNode>> result;

    public List<String> getAllIpAddresses() {
        return result.stream()
                .flatMap(result -> result.entrySet().stream())
                .filter(entry -> !"false".equals(entry.getValue().asText()))
                .flatMap(entry -> Streams.stream(entry.getValue().elements()))
                .map(JsonNode::asText)
                .collect(Collectors.toList());
    }

    public List<Map<String, JsonNode>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, JsonNode>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "MinionIpAddressesResponse{"
                + "result=" + result
                + '}';
    }
}
