package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class FlowStackEvent implements Payload {
    private Long stackId;

    public FlowStackEvent(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

}
