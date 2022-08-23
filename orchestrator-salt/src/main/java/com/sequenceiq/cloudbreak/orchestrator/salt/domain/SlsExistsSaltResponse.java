package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class SlsExistsSaltResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, Boolean>> result;

    public List<Map<String, Boolean>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Boolean>> result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "SlsExistsSaltResponse{"
                + "result=" + result
                + '}';
    }
}
