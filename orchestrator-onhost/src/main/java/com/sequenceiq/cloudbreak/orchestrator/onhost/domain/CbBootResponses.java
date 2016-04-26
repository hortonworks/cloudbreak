package com.sequenceiq.cloudbreak.orchestrator.onhost.domain;

import java.util.List;

public class CbBootResponses {

    private List<CbBootResponse> responses;

    public List<CbBootResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<CbBootResponse> responses) {
        this.responses = responses;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CbBootResponses{");
        sb.append("responses=").append(responses);
        sb.append('}');
        return sb.toString();
    }
}
