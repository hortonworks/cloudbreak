package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<TerminationEvent> {

    @Inject
    private StackUpdater stackUpdater;

    public StackTerminationAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void prepareExecution(TerminationEvent payload, Map<Object, Object> variables) {
        variables.put("FORCEDTERMINATION", payload.getForced());
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        stackUpdater.updateStackStatusWithRetry(context.getStack().getId(), DetailedStackStatus.DELETE_IN_PROGRESS,
                "Terminating FreeIPA and its infrastructure.");
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context, terminateRequest.selector(), terminateRequest);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }
}
