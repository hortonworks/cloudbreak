package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.cloud.event.Payload;

public class TestPayload implements Payload {
    private Long stackId;

    public TestPayload(Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }
}
