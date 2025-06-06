package com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.ConfigureDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.crossrealm.trust.prepare.event.PrepareIpaServerSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("PrepareCrossRealmTrustConfigureDnsAction")
public class PrepareCrossRealmTrustConfigureDnsAction extends AbstractPrepareCrossRealmTrustAction<PrepareIpaServerSuccess> {

    public PrepareCrossRealmTrustConfigureDnsAction() {
        super(PrepareIpaServerSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, PrepareIpaServerSuccess payload, Map<Object, Object> variables) throws Exception {
        stackUpdater().updateStackStatus(context.getStack(), DetailedStackStatus.PREPARE_CROSS_REALM_TRUST_IN_PROGRESS, "Configuring DNS");
        ConfigureDnsRequest request = new ConfigureDnsRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(PrepareIpaServerSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new ConfigureDnsFailed(payload.getResourceId(), ex);
    }
}
