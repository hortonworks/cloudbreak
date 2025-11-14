package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinionStatusFromFileResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<Map<String, String>> result;

    public Iterable<Map<String, String>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, String>> result) {
        this.result = result;
    }

}
