package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, JsonNode>> result;

    public Iterable<Map<String, JsonNode>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, JsonNode>> result) {
        this.result = result;
    }

    @JsonIgnore
    public String getJid() {
        if (result != null && !result.isEmpty()) {
            Map<String, JsonNode> resultMap = result.getFirst();
            if (resultMap != null && resultMap.get("jid") != null) {
                return resultMap.get("jid").asText();
            }
        }
        return null;
    }

}