package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<TerminationEvent> {

    public StackTerminationAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) {
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context, terminateRequest.selector(), terminateRequest);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }
}
