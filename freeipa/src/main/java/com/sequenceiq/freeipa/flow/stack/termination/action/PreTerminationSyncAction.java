package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.sync.PreTerminationSyncRequest;

@Component("PreTerminationSyncAction")
public class PreTerminationSyncAction extends AbstractStackTerminationAction<TerminationEvent> {

    public PreTerminationSyncAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
        variables.put("FORCEDTERMINATION", payload.getForced());
        super.prepareExecution(payload, variables);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) throws Exception {
        PreTerminationSyncRequest request = new PreTerminationSyncRequest(payload.getResourceId(), payload.getForced());
        sendEvent(context, request);
    }
}
