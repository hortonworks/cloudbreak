package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RevokeCertsRequest extends AbstractCleanupEvent {

    protected RevokeCertsRequest(Long stackId) {
        super(stackId);
    }

    public RevokeCertsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);
    }

    @JsonCreator
    public RevokeCertsRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
