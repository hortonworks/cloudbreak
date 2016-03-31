package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.domain.Stack;

@Component("TlsSetupAction")
public class TlsSetupAction extends AbstractStackCreationAction<GetSSHFingerprintsResult> {
    @Inject
    private StackCreationService stackCreationService;
    private StackContext context;
    private GetSSHFingerprintsResult payload;
    private Map<Object, Object> variables;

    public TlsSetupAction() {
        super(GetSSHFingerprintsResult.class);
    }

    @Override
    protected Long getStackId(GetSSHFingerprintsResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackContext context, GetSSHFingerprintsResult payload, Map<Object, Object> variables) throws CloudbreakException {
        this.context = context;
        this.payload = payload;
        this.variables = variables;
        Stack stack = stackCreationService.setupTls(context, payload);
        sendEvent(context);
        sendEvent(context.getFlowId(), FlowPhases.BOOTSTRAP_CLUSTER.name(), new ProvisioningContext.Builder()
                .setDefaultParams(stack.getId(), Platform.platform(stack.cloudPlatform())).build());
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new SelectableEvent(StackCreationEvent.STACK_CREATION_FINISHED_EVENT.stringRepresentation());
    }
}
