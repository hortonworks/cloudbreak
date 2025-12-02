package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Optional;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.freeipa.flow.OperationAwareAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.base.FreeIpaBaseTrustAction;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaTrustCancelState;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event.FreeIpaTrustCancelFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

public abstract class FreeIpaTrustCancelAction<P extends Payload>
        extends FreeIpaBaseTrustAction<FreeIpaTrustCancelState, FreeIpaTrustCancelFlowEvent, P>
        implements OperationAwareAction {
    protected FreeIpaTrustCancelAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustCancelFailureEvent(payload.getResourceId(), ex, ERROR);
    }
}
