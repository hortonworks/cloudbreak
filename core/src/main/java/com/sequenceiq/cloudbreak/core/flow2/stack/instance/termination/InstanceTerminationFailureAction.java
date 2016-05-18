package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

@Component("InstanceTerminationFailureAction")
public class InstanceTerminationFailureAction extends AbstractStackFailureAction<InstanceTerminationState, InstanceTerminationEvent> {
    @Inject
    private InstanceTerminationService instanceTerminationService;

    @Override
    protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
        instanceTerminationService.handleInstanceTerminationError(context.getStack(), payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new StackEvent(InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation(), context.getStack().getId());
    }
}
