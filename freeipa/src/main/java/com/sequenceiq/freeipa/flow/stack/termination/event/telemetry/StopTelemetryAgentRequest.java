package com.sequenceiq.freeipa.flow.stack.termination.event.telemetry;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class StopTelemetryAgentRequest extends TerminationEvent {

    public StopTelemetryAgentRequest(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(StopTelemetryAgentRequest.class), stackId, forced);
    }
}
