package com.sequenceiq.freeipa.flow.stack.termination.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.stack.termination.StackTerminationContext;
import com.sequenceiq.freeipa.flow.stack.termination.event.TerminationEvent;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsRequest;

@Component("DeleteUserdataSecretsAction")
public class DeleteUserdataSecretsAction extends AbstractStackTerminationAction<TerminationEvent> {

    public DeleteUserdataSecretsAction() {
        super(TerminationEvent.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminationEvent payload, Map<Object, Object> variables) throws Exception {
        DeleteUserdataSecretsRequest request = new DeleteUserdataSecretsRequest(payload.getResourceId(), payload.getForced(),
                context.getCloudContext(), context.getCloudCredential());
        sendEvent(context, request);
    }
}
