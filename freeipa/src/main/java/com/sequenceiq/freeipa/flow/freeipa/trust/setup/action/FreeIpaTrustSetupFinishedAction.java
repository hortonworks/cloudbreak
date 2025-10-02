package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FINISH_REQUIRED;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupOperationConstants.DNS_CONFIGURATION_SUCCEEDED;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FreeIpaTrustSetupFinishedAction")
public class FreeIpaTrustSetupFinishedAction extends FreeIpaTrustSetupBaseAction<StackEvent> {
    @Inject
    private TaskResultConverter taskResultConverter;

    @Inject
    private OperationService operationService;

    public FreeIpaTrustSetupFinishedAction() {
        super(StackEvent.class);
    }

    @Override
    protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        updateStatuses(
                context.getStack(),
                TRUST_SETUP_FINISH_REQUIRED,
                "Prepare cross-realm trust finished",
                TrustStatus.TRUST_SETUP_FINISH_REQUIRED
        );
        operationService.completeOperation(
                stack.getAccountId(),
                getOperationId(variables),
                List.of(taskResultConverter.convertSuccessfulTaskResult(DNS_CONFIGURATION_SUCCEEDED, stack.getEnvironmentCrn())),
                List.of()
        );
        sendEvent(context, new StackEvent(TRUST_SETUP_FINISHED_EVENT.event(), payload.getResourceId()));
    }
}
