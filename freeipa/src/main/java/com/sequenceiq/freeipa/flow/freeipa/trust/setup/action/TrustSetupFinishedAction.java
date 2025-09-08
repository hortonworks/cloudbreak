package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.OperationConstants;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("TrustSetupFinishedAction")
public class TrustSetupFinishedAction extends AbstractTrustSetupAction<StackEvent> {
    @Inject
    private TaskResultConverter taskResultConverter;

    @Inject
    private OperationService operationService;

    public TrustSetupFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        updateStatuses(context.getStack(), DetailedStackStatus.TRUST_SETUP_FINISH_REQUIRED, "Prepare cross-realm trust finished",
                TrustStatus.TRUST_SETUP_FINISH_REQUIRED);
        operationService.completeOperation(stack.getAccountId(), getOperationId(variables),
                List.of(taskResultConverter.convertSuccessfulTaskResult(OperationConstants.DNS_CONFIGURATION_SUCCEEDED, stack.getEnvironmentCrn())), List.of());
        sendEvent(context, new StackEvent(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
