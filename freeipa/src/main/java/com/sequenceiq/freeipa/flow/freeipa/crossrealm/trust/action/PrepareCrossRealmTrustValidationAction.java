package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.CrossRealmTrustValidationRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.event.PrepareCrossRealmTrustEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("PrepareCrossRealmTrustValidationAction")
public class PrepareCrossRealmTrustValidationAction extends AbstractPrepareCrossRealmTrustAction<PrepareCrossRealmTrustEvent> {

    public PrepareCrossRealmTrustValidationAction() {
        super(PrepareCrossRealmTrustEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, PrepareCrossRealmTrustEvent payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.PREPARE_CROSS_REALM_TRUST_IN_PROGRESS, "Cross-realm trust validation");
        CrossRealmTrustValidationRequest request = new CrossRealmTrustValidationRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(PrepareCrossRealmTrustEvent payload, Optional<StackContext> flowContext, Exception ex) {
        return new CrossRealmTrustValidationFailed(payload.getResourceId(), ex);
    }
}
