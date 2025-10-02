package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_IN_PROGRESS;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupFinishAction")
public class FreeIpaTrustSetupFinishAction extends FreeIpaTrustSetupFinishBaseAction<FreeIpaTrustSetupFinishEvent> {

    public FreeIpaTrustSetupFinishAction() {
        super(FreeIpaTrustSetupFinishEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupFinishEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(context.getStack(), variables, payload.getOperationId());
        updateStatuses(
                context.getStack(),
                TRUST_SETUP_FINISH_IN_PROGRESS,
                "Add cross-realm trust to FreeIPA",
                TrustStatus.TRUST_SETUP_FINISH_IN_PROGRESS
        );
        FreeIpaTrustSetupFinishAddRequest request = new FreeIpaTrustSetupFinishAddRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupFinishEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupFinishAddTrustFailed(payload.getResourceId(), ex);
    }
}
