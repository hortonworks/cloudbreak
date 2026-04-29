package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupAddTrustAction")
public class FreeIpaTrustSetupAddTrustAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupUpdatePillarDataSuccess> {

    public FreeIpaTrustSetupAddTrustAction() {
        super(FreeIpaTrustSetupUpdatePillarDataSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupUpdatePillarDataSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), TRUST_SETUP_IN_PROGRESS, "Adding cross-realm trust", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        FreeIpaTrustSetupAddTrustRequest request = new FreeIpaTrustSetupAddTrustRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupUpdatePillarDataSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupAddTrustFailed(payload.getResourceId(), ex, ERROR);
    }
}

