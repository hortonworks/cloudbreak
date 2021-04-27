package com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class RemoveReplicationAgreementsResponse extends StackEvent {

    public RemoveReplicationAgreementsResponse(Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "RemoveReplicationAgreementsResponse{" +
                super.toString() +
                '}';
    }
}
