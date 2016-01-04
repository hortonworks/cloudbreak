package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.setup.SetupRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.service.stack.event.ProvisionRequest;

@Component("ProvisioningSetupAction")
public class ProvisioningSetupAction extends AbstractStackCreationAction<ProvisionRequest> {
    public ProvisioningSetupAction() {
        super(ProvisionRequest.class);
    }

    protected void doExecute(StackContext context, ProvisionRequest provisionRequest, Map<Object, Object> variables) {
        sendEvent(context.getFlowId(), new SetupRequest<SetupResult>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack()));
    }

    @Override
    protected Long getStackId(ProvisionRequest payload) {
        return payload.getStackId();
    }
}
