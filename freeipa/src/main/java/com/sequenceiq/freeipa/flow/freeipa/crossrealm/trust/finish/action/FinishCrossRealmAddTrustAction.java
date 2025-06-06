package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustAddTrustRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.finish.event.FinishCrossRealmTrustEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FinishCrossRealmAddTrustAction")
public class FinishCrossRealmAddTrustAction extends AbstractFinishCrossRealmTrustAction<FinishCrossRealmTrustEvent> {

    public FinishCrossRealmAddTrustAction() {
        super(FinishCrossRealmTrustEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, FinishCrossRealmTrustEvent payload, Map<Object, Object> variables) throws Exception {
        setOperationId(variables, payload.getOperationId());
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.FINISH_CROSS_REALM_TRUST_IN_PROGRESS, "Add cross-realm trust to FreeIPA");
        FinishCrossRealmTrustAddTrustRequest request = new FinishCrossRealmTrustAddTrustRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FinishCrossRealmTrustEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new FinishCrossRealmTrustAddTrustFailed(payload.getResourceId(), ex);
    }
}
