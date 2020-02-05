package com.sequenceiq.freeipa.flow.stack.termination.event.telemetry;

import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

public class StopTelemetryAgentFinished extends TerminationEvent {

    public StopTelemetryAgentFinished(Long stackId, Boolean forced) {
        super(EventSelectorUtil.selector(StopTelemetryAgentFinished.class), stackId, forced);
    }
}
