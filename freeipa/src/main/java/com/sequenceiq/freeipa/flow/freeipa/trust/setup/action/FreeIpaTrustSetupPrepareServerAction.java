package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupPrepareServerAction")
public class FreeIpaTrustSetupPrepareServerAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupValidationSuccess> {

    public FreeIpaTrustSetupPrepareServerAction() {
        super(FreeIpaTrustSetupValidationSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupValidationSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), TRUST_SETUP_IN_PROGRESS, "Prepare IPA server", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        updateOperation(context.getStack(), getOperationId(variables), payload.getTaskResults());
        FreeIpaTrustSetupPrepareServerRequest request = new FreeIpaTrustSetupPrepareServerRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupValidationSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupPrepareServerFailed(payload.getResourceId(), ex);
    }
}
