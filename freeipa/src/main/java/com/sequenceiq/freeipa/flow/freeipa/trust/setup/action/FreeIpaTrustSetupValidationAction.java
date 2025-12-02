package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.VALIDATION;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupValidationRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupValidationAction")
public class FreeIpaTrustSetupValidationAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupEvent> {

    public FreeIpaTrustSetupValidationAction() {
        super(FreeIpaTrustSetupEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(context.getStack(), variables, payload.getOperationId());
        updateStatuses(
                context.getStack(),
                TRUST_SETUP_IN_PROGRESS,
                "Cross-realm trust validation",
                TrustStatus.TRUST_SETUP_IN_PROGRESS
        );
        FreeIpaTrustSetupValidationRequest request = new FreeIpaTrustSetupValidationRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupValidationFailed(payload.getResourceId(), ex, VALIDATION);
    }
}
