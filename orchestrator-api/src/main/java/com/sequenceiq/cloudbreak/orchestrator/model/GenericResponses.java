package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.List;

public class GenericResponses {

    private List<GenericResponse> responses;

    public List<GenericResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<GenericResponse> responses) {
        this.responses = responses;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SaltBootResponses{");
        sb.append("responses=").append(responses);
        sb.append('}');
        return sb.toString();
    }
}
