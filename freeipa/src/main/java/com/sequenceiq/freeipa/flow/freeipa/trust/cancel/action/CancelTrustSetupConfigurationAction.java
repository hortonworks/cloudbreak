package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupConfigurationRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("CancelTrustSetupConfigurationAction")
public class CancelTrustSetupConfigurationAction extends AbstractCancelTrustSetupAction<CancelTrustSetupEvent> {

    public CancelTrustSetupConfigurationAction() {
        super(CancelTrustSetupEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, CancelTrustSetupEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(context.getStack(), variables, payload.getOperationId());
        updateStatuses(context.getStack(), DetailedStackStatus.CANCEL_TRUST_SETUP_IN_PROGRESS, "Cancel cross-realm trust setup",
                TrustStatus.CANCEL_TRUST_SETUP_IN_PROGRESS);
        CancelTrustSetupConfigurationRequest request = new CancelTrustSetupConfigurationRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(CancelTrustSetupEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new CancelTrustSetupConfigurationFailed(payload.getResourceId(), ex);
    }
}
