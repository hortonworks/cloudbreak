package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinionKeysOnMasterResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, JsonNode>> result;

    @JsonIgnore
    public List<String> getUnacceptedMinions() {
        return getMinionsByState("minions_pre");
    }

    @JsonIgnore
    public List<String> getAcceptedMinions() {
        return getMinionsByState("minions");
    }

    @JsonIgnore
    public List<String> getDeniedMinions() {
        return getMinionsByState("minions_denied");
    }

    @JsonIgnore
    public List<String> getRejectedMinions() {
        return getMinionsByState("minions_rejected");
    }

    @JsonIgnore
    public List<String> getAllMinions() {
        return Stream.of(getAcceptedMinions(), getUnacceptedMinions(), getDeniedMinions(), getRejectedMinions())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<String> getMinionsByState(String minionByState) {
        return result.stream()
                .flatMap(result -> result.entrySet().stream())
                .filter(entry -> "data".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(dataNode -> dataNode.get("return"))
                .filter(Objects::nonNull)
                .map(minions -> minions.get(minionByState))
                .filter(Objects::nonNull)
                .filter(JsonNode::isArray)
                .flatMap(minions -> StreamSupport.stream(minions.spliterator(), false))
                .map(JsonNode::textValue)
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
        return "MinionKeysOnMasterResponse{"
                + "result=\n" + result + '\n'
                + '}';
    }
}
