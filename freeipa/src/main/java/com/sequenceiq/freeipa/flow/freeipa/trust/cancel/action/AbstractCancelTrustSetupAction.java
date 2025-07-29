package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.action.AbstractTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.CancelTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class AbstractCancelTrustSetupAction<P extends Payload>
        extends AbstractTrustAction<FreeIpaCancelTrustSetupState, FreeIpaCancelTrustSetupFlowEvent, P>
        implements OperationAwareAction {
    protected AbstractCancelTrustSetupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new CancelTrustSetupFailureEvent(payload.getResourceId(), ex);
    }
}
