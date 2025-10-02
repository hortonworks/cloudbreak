package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.base.FreeIpaBaseTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.FreeIpaTrustSetupFinishState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class FreeIpaTrustSetupFinishBaseAction<P extends Payload>
        extends FreeIpaBaseTrustAction<FreeIpaTrustSetupFinishState, FreeIpaTrustSetupFinishFlowEvent, P>
        implements OperationAwareAction {
    protected FreeIpaTrustSetupFinishBaseAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupFinishFailureEvent(payload.getResourceId(), ex);
    }
}
