package com.sequenceiq.cloudbreak.core.flow2.stack;

public class FlowStackEvent {
    private Long stackId;

    public FlowStackEvent(Long stackId) {
        this.stackId = stackId;
    }

    public Long getStackId() {
        return stackId;
    }
}
