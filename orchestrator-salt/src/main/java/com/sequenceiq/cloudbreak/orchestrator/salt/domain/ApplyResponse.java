package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyResponse {

    @JsonProperty("return")
    private List<Map<String, Object>> result;

    public List<Map<String, Object>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Object>> result) {
        this.result = result;
    }

    public String getJid() {
        return result.get(0).get("jid").toString();
    }

    @Override
    public String toString() {
        return "ApplyResponse{"
                + "result=" + result
                + '}';
    }
}
