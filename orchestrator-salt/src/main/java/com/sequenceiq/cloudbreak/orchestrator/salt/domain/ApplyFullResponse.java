package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyFullResponse {

    private boolean error;

    @JsonProperty("return")
    private List<Map<String, FullNodeResponse>> result;

    public Iterable<Map<String, FullNodeResponse>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, FullNodeResponse>> result) {
        this.result = result;
        setError();
    }

    public boolean isError() {
        return error;
    }

    private void setError() {
        if (CollectionUtils.isNotEmpty(result)) {
            ArrayList<Map<String, FullNodeResponse>> results = Lists.newArrayList(result);
            error = results.stream().anyMatch(allNodeResults ->
                    allNodeResults.values().stream().anyMatch(nodeResponse -> nodeResponse.getRetcode() != 0));
        }
    }

    @Override
    public String toString() {
        return "ApplyFullResponse{" +
                "error=" + error +
                ", result=" + result +
                '}';
    }
}
