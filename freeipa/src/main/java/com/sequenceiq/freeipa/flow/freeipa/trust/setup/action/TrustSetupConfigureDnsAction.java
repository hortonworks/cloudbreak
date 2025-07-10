package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.ConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.ConfigureDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.PrepareIpaServerSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("TrustSetupConfigureDnsAction")
public class TrustSetupConfigureDnsAction extends AbstractTrustSetupAction<PrepareIpaServerSuccess> {

    public TrustSetupConfigureDnsAction() {
        super(PrepareIpaServerSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, PrepareIpaServerSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), DetailedStackStatus.TRUST_SETUP_IN_PROGRESS, "Configuring DNS", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        ConfigureDnsRequest request = new ConfigureDnsRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(PrepareIpaServerSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new ConfigureDnsFailed(payload.getResourceId(), ex);
    }
}
