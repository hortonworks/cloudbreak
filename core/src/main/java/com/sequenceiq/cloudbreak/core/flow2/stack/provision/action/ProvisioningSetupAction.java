package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;

@Component("ProvisioningSetupAction")
public class ProvisioningSetupAction extends AbstractStackCreationAction<ProvisioningContext> {
    public ProvisioningSetupAction() {
        super(ProvisioningContext.class);
    }

    protected void doExecute(StackContext context, ProvisioningContext provisioningContext, Map<Object, Object> variables) {
        sendEvent(context.getFlowId(), new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
    }

    @Override
    protected Long getStackId(ProvisioningContext payload) {
        return payload.getStackId();
    }
}
