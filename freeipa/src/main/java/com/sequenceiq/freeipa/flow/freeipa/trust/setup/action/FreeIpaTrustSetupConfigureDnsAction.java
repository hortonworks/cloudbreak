package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupOperationConstants.IPASERVER_PREPARATION_SUCCEEDED;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupPrepareServerSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupConfigureDnsAction")
public class FreeIpaTrustSetupConfigureDnsAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupPrepareServerSuccess> {

    public FreeIpaTrustSetupConfigureDnsAction() {
        super(FreeIpaTrustSetupPrepareServerSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupPrepareServerSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), TRUST_SETUP_IN_PROGRESS, "Configuring DNS", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        updateOperation(context.getStack(), getOperationId(variables), List.of(IPASERVER_PREPARATION_SUCCEEDED));
        FreeIpaTrustSetupConfigureDnsRequest request = new FreeIpaTrustSetupConfigureDnsRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupPrepareServerSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupConfigureDnsFailed(payload.getResourceId(), ex, ERROR);
    }
}
