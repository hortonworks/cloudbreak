package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

import java.util.List;

public class SaltBootResponses {

    private List<SaltBootResponse> responses;

    public List<SaltBootResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<SaltBootResponse> responses) {
        this.responses = responses;
    }

    public void assertError() throws CloudbreakOrchestratorFailedException {
        for (SaltBootResponse resp : responses) {
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
