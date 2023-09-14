package com.sequenceiq.common.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UsedSubnetsByEnvironmentResponse {

    private List<UsedSubnetWithResourceResponse> responses;

    public UsedSubnetsByEnvironmentResponse() {
    }

    @JsonCreator
    public UsedSubnetsByEnvironmentResponse(@JsonProperty("responses") List<UsedSubnetWithResourceResponse> responses) {
        this.responses = responses;
    }

    public List<UsedSubnetWithResourceResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<UsedSubnetWithResourceResponse> responses) {
        this.responses = responses;
    }

    @Override
    public String toString() {
        return "UsedSubnetsByEnvironmentResponse{" +
                "responses=" + responses +
                '}';
    }
}
