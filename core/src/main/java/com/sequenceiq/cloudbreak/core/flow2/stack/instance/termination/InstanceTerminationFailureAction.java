package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;

@Component("InstanceTerminationFailureAction")
public class InstanceTerminationFailureAction extends AbstractStackFailureAction<InstanceTerminationState, InstanceTerminationEvent> {
    @Inject
    private InstanceTerminationService instanceTerminationService;

    @Override
    protected void doExecute(StackFailureContext context, FlowFailureEvent payload, Map<Object, Object> variables) {
        instanceTerminationService.handleInstanceTerminationError(context.getStack(), payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackFailureContext context) {
        return new SelectableFlowStackEvent(context.getStack().getId(), InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation());
    }
}
