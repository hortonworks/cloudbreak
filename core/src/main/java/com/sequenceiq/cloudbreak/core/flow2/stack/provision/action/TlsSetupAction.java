package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetSSHFingerprintsResult;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
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
    }

    @Override
    protected Selectable createRequest(StackContext context) {
        return new SelectableFlowStackEvent(context.getStack().getId(), StackCreationEvent.STACK_CREATION_FINISHED_EVENT.stringRepresentation());
    }
}
