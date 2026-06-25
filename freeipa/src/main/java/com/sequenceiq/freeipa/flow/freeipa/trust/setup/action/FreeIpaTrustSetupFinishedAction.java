package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_SETUP_TRUST_FINISHED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_REQUIRED;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupOperationConstants.PILLAR_UPDATE_SUCCEEDED;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.util.TrustRelationshipType;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupAddTrustSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FreeIpaTrustSetupFinishedAction")
public class FreeIpaTrustSetupFinishedAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupAddTrustSuccess> {

    @Inject
    private TaskResultConverter taskResultConverter;

    @Inject
    private OperationService operationService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    @Inject
    private EnvironmentService environmentService;

    public FreeIpaTrustSetupFinishedAction() {
        super(FreeIpaTrustSetupAddTrustSuccess.class);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupAddTrustSuccess payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        EnvironmentType environmentType = environmentService.getEnvironmentType(stack.getEnvironmentCrn());
        if (environmentType.isHybrid()) {
            updateStatuses(stack, TRUST_SETUP_FINISH_REQUIRED, "Prepare cross-realm trust finished", TrustStatus.TRUST_SETUP_FINISH_REQUIRED);
        } else {
            updateStatuses(stack, AVAILABLE, "Cross-realm trust setup finished", TrustStatus.TRUST_SETUP_FINISH_REQUIRED);
        }
        crossRealmTrustService.updateTrustRelationshipTypeByStackId(stack.getId(), TrustRelationshipType.ONE_WAY);
        getEventService().sendEventAndNotification(stack, context.getFlowTriggerUserCrn(), FREEIPA_SETUP_TRUST_FINISHED);
        operationService.completeOperation(stack.getAccountId(), getOperationId(variables),
                List.of(taskResultConverter.convertSuccessfulTaskResult(PILLAR_UPDATE_SUCCEEDED, stack.getEnvironmentCrn())), List.of());
        sendEvent(context, new StackEvent(TRUST_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
