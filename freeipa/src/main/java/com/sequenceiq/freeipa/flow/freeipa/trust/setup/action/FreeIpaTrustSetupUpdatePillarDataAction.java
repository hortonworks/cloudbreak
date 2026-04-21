package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupOperationConstants.DNS_CONFIGURE_SUCCEEDED;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupConfigureDnsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataFailed;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupUpdatePillarDataRequest;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@Component("FreeIpaTrustSetupUpdatePillarDataAction")
public class FreeIpaTrustSetupUpdatePillarDataAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupConfigureDnsSuccess> {

    protected FreeIpaTrustSetupUpdatePillarDataAction() {
        super(FreeIpaTrustSetupConfigureDnsSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupConfigureDnsSuccess payload, Map<Object, Object> variables) throws Exception {
        updateStatuses(context.getStack(), TRUST_SETUP_IN_PROGRESS, "Updating pillar data", TrustStatus.TRUST_SETUP_IN_PROGRESS);
        updateOperation(context.getStack(), getOperationId(variables), List.of(DNS_CONFIGURE_SUCCEEDED));
        FreeIpaTrustSetupUpdatePillarDataRequest request = new FreeIpaTrustSetupUpdatePillarDataRequest(payload.getResourceId());
        sendEvent(context, request.selector(), request);
    }

    @Override
    protected Object getFailurePayload(FreeIpaTrustSetupConfigureDnsSuccess payload, Optional<StackContext> flowContext, Exception ex) {
        return new FreeIpaTrustSetupUpdatePillarDataFailed(payload.getResourceId(), ex);
    }
}
