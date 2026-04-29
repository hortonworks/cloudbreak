package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishValidateTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishValidateTrustRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupFinishValidateTrustAction")
public class FreeIpaTrustSetupFinishValidateTrustAction extends FreeIpaTrustSetupFinishBaseAction<FreeIpaTrustSetupFinishAddTrustSuccess> {

    public FreeIpaTrustSetupFinishValidateTrustAction() {
        super(FreeIpaTrustSetupFinishAddTrustSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupFinishAddTrustSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), TRUST_SETUP_FINISH_IN_PROGRESS, "Validating cross-realm trust", TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS);
        FreeIpaTrustSetupFinishValidateTrustRequest request = new FreeIpaTrustSetupFinishValidateTrustRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupFinishAddTrustSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupFinishValidateTrustFailed(payload.getResourceId(), ex, ERROR);
    }
}
