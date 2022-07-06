package com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.AbstractCleanupEvent;

public class RemoveDnsRequest extends AbstractCleanupEvent {

    protected RemoveDnsRequest(Long stackId) {
        super(stackId);
    }

    public RemoveDnsRequest(CleanupEvent cleanupEvent) {
        super(cleanupEvent);    }

    @JsonCreator
    public RemoveDnsRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("cleanupEvent") CleanupEvent cleanupEvent) {
        super(selector, cleanupEvent);
    }
}
