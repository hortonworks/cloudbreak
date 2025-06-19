package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("TrustSetupValidationAction")
public class TrustSetupValidationAction extends AbstractTrustSetupAction<TrustSetupEvent> {

    public TrustSetupValidationAction() {
        super(TrustSetupEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, TrustSetupEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(variables, payload.getOperationId());
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.TRUST_SETUP_IN_PROGRESS, "Cross-realm trust validation");
        TrustSetupValidationRequest request = new TrustSetupValidationRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(TrustSetupEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new TrustSetupValidationFailed(payload.getResourceId(), ex);
    }
}
