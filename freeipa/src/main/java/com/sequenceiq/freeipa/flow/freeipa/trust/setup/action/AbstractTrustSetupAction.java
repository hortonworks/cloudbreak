package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.action.AbstractTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class AbstractTrustSetupAction<P extends Payload>
        extends AbstractTrustAction<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent, P>
        implements OperationAwareAction {
    protected AbstractTrustSetupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new TrustSetupFailureEvent(payload.getResourceId(), ex);
    }
}
