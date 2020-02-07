package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.telemetry.StopTelemetryAgentRequest;

@Component("StopTelemetryAgentAction")
public class StopTelemetryAgentAction extends AbstractStackTerminationAction<TerminationEvent> {

    public StopTelemetryAgentAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) throws Exception {
        StopTelemetryAgentRequest request = new StopTelemetryAgentRequest(payload.getResourceId(), payload.getForced());
        sendEvent(context, request);
    }
}
