package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsRequest;

@Component("DeleteUserdataSecretsAction")
public class DeleteUserdataSecretsAction extends AbstractStackTerminationAction<StackPreTerminationSuccess> {

    public DeleteUserdataSecretsAction() {
        super(StackPreTerminationSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, StackPreTerminationSuccess payload, Map<Object, Object> variables) throws Exception {
        DeleteUserdataSecretsRequest request = new DeleteUserdataSecretsRequest(payload.getResourceId(), payload.getTerminationType(),
                context.getCloudContext(), context.getCloudCredential());
        sendEvent(context, request);
    }
}
