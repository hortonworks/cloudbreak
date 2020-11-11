package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyResponse {

    @JsonProperty("return")
    private List<Map<String, JsonNode>> result;

    public Iterable<Map<String, JsonNode>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, JsonNode>> result) {
        this.result = result;
    }

    public String getJid() {
        if (result != null && !result.isEmpty()) {
            Map<String, JsonNode> resultMap = result.get(0);
            if (resultMap != null && resultMap.get("jid") != null) {
                return resultMap.get("jid").asText();
            }
        }
        return null;
    }

}
