package com.sequenceiq.freeipa.flow.stack.termination.event.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class StopTelemetryAgentRequest extends TerminationEvent {

    @JsonCreator
    public StopTelemetryAgentRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("forced") Boolean forced) {
        super(EventSelectorUtil.selector(StopTelemetryAgentRequest.class), stackId, forced);
    }
}
