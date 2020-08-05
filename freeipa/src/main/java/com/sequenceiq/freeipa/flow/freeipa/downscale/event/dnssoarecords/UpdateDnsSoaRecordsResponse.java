package com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpdateDnsSoaRecordsResponse extends StackEvent {

    public UpdateDnsSoaRecordsResponse(Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "UpdateDnsSoaRecordsResponse{" +
                super.toString() +
                '}';
    }
}