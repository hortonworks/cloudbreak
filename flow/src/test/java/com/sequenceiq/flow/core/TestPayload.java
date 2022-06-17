package com.sequenceiq.flow.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.Payload;

public class TestPayload implements Payload {
    private final Long stackId;

    @JsonCreator
    public TestPayload(@JsonProperty("resourceId") Long stackId) {
        this.stackId = stackId;
    }

    @Override
    public Long getResourceId() {
        return stackId;
    }
}
