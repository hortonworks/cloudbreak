package com.sequenceiq.cloudbreak.core.flow2.stack.repair;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.springframework.statemachine.StateContext;

import javax.inject.Inject;
import java.util.Optional;

public abstract class AbstractStackRepairTriggerAction<P extends Payload>
        extends AbstractAction<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent, StackRepairTriggerContext, P> {

    @Inject
    private StackService stackService;

    protected AbstractStackRepairTriggerAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected StackRepairTriggerContext createFlowContext(
            String flowId, StateContext<ManualStackRepairTriggerState, ManualStackRepairTriggerEvent> stateContext, P payload) {
        Long stackId = payload.getStackId();
        Stack stack = stackService.getById(stackId);
        return new StackRepairTriggerContext(flowId, stack);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackRepairTriggerContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getStackId(), ex);
    }
}
