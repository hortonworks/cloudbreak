package com.sequenceiq.cloudbreak.orchestrator.model;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

import java.util.List;

public class GenericResponses {

    private List<GenericResponse> responses;

    public List<GenericResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<GenericResponse> responses) {
        this.responses = responses;
    }

    public void assertError() throws CloudbreakOrchestratorFailedException {
        for (GenericResponse resp : responses) {
            resp.assertError();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SaltBootResponses{");
        sb.append("responses=").append(responses);
        sb.append('}');
        return sb.toString();
    }
}
