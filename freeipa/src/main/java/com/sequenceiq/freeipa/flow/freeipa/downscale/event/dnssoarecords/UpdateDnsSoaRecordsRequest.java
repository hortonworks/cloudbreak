package com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class UpdateDnsSoaRecordsRequest extends AbstractCleanupEvent {

    protected UpdateDnsSoaRecordsRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public UpdateDnsSoaRecordsRequest(
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }
}
