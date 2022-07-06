package com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpdateDnsSoaRecordsResponse extends StackEvent {

    @JsonCreator
    public UpdateDnsSoaRecordsResponse(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }

    @Override
    public String toString() {
        return "UpdateDnsSoaRecordsResponse{" +
                super.toString() +
                '}';
    }
}
