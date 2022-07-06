package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RemoveReplicationAgreementsResponse extends StackEvent {

    @JsonCreator
    public RemoveReplicationAgreementsResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "RemoveReplicationAgreementsResponse{" +
                super.toString() +
                '}';
    }
}
