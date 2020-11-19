package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinionFingersOnMasterResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, JsonNode>> result;

    @JsonIgnore
    public Map<String, String> getUnacceptedMinions() {
        Optional<JsonNode> minionsWithFingerprint = mapToMinionsWithFingerprint();
        if (minionsWithFingerprint.isPresent()) {
            return collectFingerprintByMinion(minionsWithFingerprint.get());
        } else {
            return Map.of();
        }
    }

    private Map<String, String> collectFingerprintByMinion(JsonNode jsonNode) {
        Iterator<Entry<String, JsonNode>> fields = jsonNode.fields();
        Map<String, String> fingerprintByMinion = new HashMap<>();
        for (Iterator<Entry<String, JsonNode>> it = fields; it.hasNext();) {
            Entry<String, JsonNode> field = it.next();
            fingerprintByMinion.put(field.getKey(), field.getValue().textValue());
        }
        return fingerprintByMinion;
    }

    private Optional<JsonNode> mapToMinionsWithFingerprint() {
        return result.stream()
                .flatMap(result -> result.entrySet().stream())
                .filter(entry -> "data".equals(entry.getKey()))
                .map(Entry::getValue)
                .map(dataNode -> dataNode.get("return"))
                .filter(Objects::nonNull)
                .map(minions -> minions.get("minions_pre"))
                .filter(Objects::nonNull)
                .filter(JsonNode::isObject)
                .findFirst();
    }

    public List<Map<String, JsonNode>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, JsonNode>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "MinionFingersOnMasterResponse{"
                + "result=\n" + result + '\n'
                + '}';
    }
}
