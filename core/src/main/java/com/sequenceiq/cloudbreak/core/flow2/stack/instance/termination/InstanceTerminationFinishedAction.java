package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component("InstanceTerminationFinishedAction")
public class InstanceTerminationFinishedAction extends AbstractInstanceTerminationAction<RemoveInstanceResult> {
    @Inject
    private InstanceTerminationService instanceTerminationService;

    public InstanceTerminationFinishedAction() {
        super(RemoveInstanceResult.class);
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, RemoveInstanceResult payload, Map<Object, Object> variables)
            throws TransactionExecutionException {
        instanceTerminationService.finishInstanceTermination(context, payload);
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new StackEvent(InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT.event(), context.getStack().getId());
    }
}
