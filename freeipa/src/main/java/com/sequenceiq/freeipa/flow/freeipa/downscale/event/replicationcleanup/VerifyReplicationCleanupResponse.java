package com.sequenceiq.freeipa.flow.freeipa.downscale.event.replicationcleanup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class VerifyReplicationCleanupResponse extends StackEvent {

    @JsonCreator
    public VerifyReplicationCleanupResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "VerifyReplicationCleanupResponse{" +
                super.toString() +
                '}';
    }
}
