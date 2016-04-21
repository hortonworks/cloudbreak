package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;

@Component("InstanceTerminationFinishedAction")
public class InstanceTerminationFinishedAction extends AbstractInstanceTerminationAction<RemoveInstanceResult> {
    @Inject
    private InstanceTerminationService instanceTerminationService;

    public InstanceTerminationFinishedAction() {
        super(RemoveInstanceResult.class);
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, RemoveInstanceResult payload, Map<Object, Object> variables) throws Exception {
        instanceTerminationService.finishInstanceTermination(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new SelectableFlowStackEvent(context.getStack().getId(), InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation());
    }
}
