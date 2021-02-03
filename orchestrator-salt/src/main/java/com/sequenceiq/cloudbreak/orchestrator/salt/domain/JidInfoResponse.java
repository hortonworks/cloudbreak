package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JidInfoResponse {

    @JsonProperty("return")
    @SerializedName("return")
    private List<JsonNode> returnResponse;

    public List<JsonNode> getReturn() {
        return returnResponse;
    }

    public void setReturn(List<JsonNode> returnResponse) {
        this.returnResponse = returnResponse;
    }

    @JsonIgnore
    public boolean hasDataFieldInResult() {
        return returnResponse.get(0).has("data");
    }

    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(returnResponse)
                || returnResponse.get(0) == null
                || returnResponse.get(0).isArray()
                || getResults().isEmpty();
    }

    @JsonIgnore
    public JsonNode getResults() {
        if (hasDataFieldInResult()) {
            return returnResponse.get(0).get("data");
        } else {
            return returnResponse.get(0);
        }
    }

    @Override
    public String toString() {
        return "JidInfoResponse{" +
                "returnResponse=" + returnResponse +
                '}';
    }
}
