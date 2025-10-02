package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.base.FreeIpaBaseTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class FreeIpaTrustSetupBaseAction<P extends Payload>
        extends FreeIpaBaseTrustAction<FreeIpaTrustSetupState, FreeIpaTrustSetupFlowEvent, P>
        implements OperationAwareAction {
    protected FreeIpaTrustSetupBaseAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupFailureEvent(payload.getResourceId(), ex);
    }
}
