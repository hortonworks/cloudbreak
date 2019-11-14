package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.ums.RemoveMachineUserRequest;

@Component("MachineUserRemoveAction")
public class MachineUserRemoveAction extends AbstractStackTerminationAction<TerminationEvent> {

    public MachineUserRemoveAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) throws Exception {
        RemoveMachineUserRequest request = new RemoveMachineUserRequest(payload.getResourceId(), payload.getForced());
        sendEvent(context, request);
    }
}
