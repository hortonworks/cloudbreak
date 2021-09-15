package com.sequenceiq.flow.domain;

import com.sequenceiq.flow.api.model.FlowIdentifier;

public class RetryResponse {

    private final String name;

    private final FlowIdentifier flowIdentifier;

    public RetryResponse(String name, FlowIdentifier flowIdentifier) {
        this.name = name;
        this.flowIdentifier = flowIdentifier;
    }

    public String getName() {
        return name;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    @Override
    public String toString() {
        return "RetryResponse{" +
                "name='" + name + '\'' +
                ", flowIdentifier=" + flowIdentifier +
                '}';
    }
}
