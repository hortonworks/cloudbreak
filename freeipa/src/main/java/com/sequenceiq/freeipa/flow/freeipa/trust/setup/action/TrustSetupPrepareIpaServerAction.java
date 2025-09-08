package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupValidationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("TrustSetupPrepareIpaServerAction")
public class TrustSetupPrepareIpaServerAction extends AbstractTrustSetupAction<TrustSetupValidationSuccess> {

    public TrustSetupPrepareIpaServerAction() {
        super(TrustSetupValidationSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, TrustSetupValidationSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), DetailedStackStatus.TRUST_SETUP_IN_PROGRESS, "Prepare IPA server", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        updateOperation(context.getStack(), getOperationId(variables), payload.getTaskResults());
        PrepareIpaServerRequest request = new PrepareIpaServerRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(TrustSetupValidationSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new PrepareIpaServerFailed(payload.getResourceId(), ex);
    }
}
