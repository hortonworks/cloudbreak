package com.sequenceiq.freeipa.flow.freeipa.trust.finish.action;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.action.AbstractTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.event.FinishTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class AbstractFinishTrustSetupAction<P extends Payload>
        extends AbstractTrustAction<FreeIpaFinishTrustSetupState, FreeIpaFinishTrustSetupFlowEvent, P>
        implements OperationAwareAction {
    protected AbstractFinishTrustSetupAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FinishTrustSetupFailureEvent(payload.getResourceId(), ex);
    }
}
