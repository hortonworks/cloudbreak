package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelConfigurationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelConfigurationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustCancelConfigurationAction")
public class FreeIpaTrustCancelConfigurationAction extends FreeIpaTrustCancelAction<FreeIpaTrustCancelEvent> {

    protected FreeIpaTrustCancelConfigurationAction() {
        super(FreeIpaTrustCancelEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustCancelEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(context.getStack(), variables, payload.getOperationId());
        updateStatuses(
                context.getStack(),
                DetailedStackStatus.CANCEL_TRUST_SETUP_IN_PROGRESS,
                "Cancel cross-realm trust setup",
                TrustStatus.CANCEL_TRUST_SETUP_IN_PROGRESS
        );
        FreeIpaTrustCancelConfigurationRequest request = new FreeIpaTrustCancelConfigurationRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustCancelEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustCancelConfigurationFailed(payload.getResourceId(), ex);
    }
}
