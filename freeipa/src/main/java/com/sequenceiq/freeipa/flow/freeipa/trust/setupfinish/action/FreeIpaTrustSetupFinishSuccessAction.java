package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_SUCCESSFUL;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus.TRUST_ACTIVE;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event.FreeIpaTrustSetupFinishFlowEvent.TRUST_SETUP_FINISH_FINISHED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.sync.crossrealmtrust.CrossRealmTrustStatusSyncJobService;

@Component("FreeIpaTrustSetupFinishSuccessAction")
public class FreeIpaTrustSetupFinishSuccessAction extends FreeIpaTrustSetupFinishBaseAction<StackEvent> {

    @Inject
    private OperationService operationService;

    @Inject
    private CrossRealmTrustStatusSyncJobService crossRealmTrustStatusSyncJobService;

    public FreeIpaTrustSetupFinishSuccessAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        updateStatuses(
                stack,
                TRUST_SETUP_FINISH_SUCCESSFUL,
                "Finish setting up cross-realm trust was successful",
                TRUST_ACTIVE
        );
        operationService.completeOperation(
                stack.getAccountId(),
                getOperationId(variables),
                List.of(new SuccessDetails(stack.getEnvironmentCrn())),
                List.of()
        );
        crossRealmTrustStatusSyncJobService.schedule(stack.getId());
        sendEvent(context, new StackEvent(TRUST_SETUP_FINISH_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
