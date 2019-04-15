package com.sequenceiq.cloudbreak.core.flow2;

public class CommonContext {

    private final String flowId;

    public CommonContext(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowId() {
        return flowId;
    }
}
