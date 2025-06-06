package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.CrossRealmTrustValidationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareIpaServerFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareIpaServerRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("PrepareCrossRealmTrustPrepareIpaServerAction")
public class PrepareCrossRealmTrustPrepareIpaServerAction extends AbstractPrepareCrossRealmTrustAction<CrossRealmTrustValidationSuccess> {

    public PrepareCrossRealmTrustPrepareIpaServerAction() {
        super(CrossRealmTrustValidationSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, CrossRealmTrustValidationSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.PREPARE_CROSS_REALM_TRUST_IN_PROGRESS, "Prepare IPA server");
        PrepareIpaServerRequest request = new PrepareIpaServerRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(CrossRealmTrustValidationSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new PrepareIpaServerFailed(payload.getResourceId(), ex);
    }
}
