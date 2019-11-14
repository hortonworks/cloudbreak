package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<CcmKeyDeregisterSuccess> {

    public StackTerminationAction() {
        super(CcmKeyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, CcmKeyDeregisterSuccess payload, Map<Object, Object> variables) {
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context, terminateRequest.selector(), terminateRequest);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }
}
