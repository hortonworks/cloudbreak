package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.TrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("TrustSetupFailedAction")
public class TrustSetupFailedAction extends AbstractTrustSetupAction<TrustSetupFailureEvent> {

    @Inject
    private OperationService operationService;

    @Inject
    private TaskResultConverter taskResultConverter;

    protected TrustSetupFailedAction() {
        super(TrustSetupFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaTrustSetupState,
            FreeIpaTrustSetupFlowEvent> stateContext, TrustSetupFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, TrustSetupFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to prepare cross-realm trust FreeIPA: " + getErrorReason(payload);
        updateStatuses(context.getStack(), DetailedStackStatus.TRUST_SETUP_FAILED, statusReason, TrustStatus.TRUST_SETUP_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason,
                taskResultConverter.convertSuccessfulTasks(payload.getTaskResults().getSuccessfulTasks(), stack.getEnvironmentCrn()),
                getFailureDetails(payload, stack));
        sendEvent(context, new StackEvent(FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }

    private List<FailureDetails> getFailureDetails(TrustSetupFailureEvent payload, Stack stack) {
        if (payload.getTaskResults().hasErrors()) {
            return taskResultConverter.convertFailedTasks(payload.getTaskResults().getErrors(), stack.getEnvironmentCrn());
        } else {
            return List.of(taskResultConverter.convertFailedTaskResult(payload.getException().getMessage(), stack.getEnvironmentCrn()));
        }
    }

    protected String getErrorReason(TrustSetupFailureEvent payload) {
        if (payload.getTaskResults().hasErrors()) {
            List<TaskResult> errors = payload.getTaskResults().getErrors();
            return IntStream.range(0, errors.size())
                    .mapToObj(i -> getNumberingIfRequired(errors.size(), i) + errors.get(i).message())
                    .collect(Collectors.joining("\n"));
        } else {
            return getErrorReason(payload.getException());
        }
    }

    private String getNumberingIfRequired(int errorSize, int i) {
        return errorSize == 1 ? "" : i + 1 + ". ";
    }
}
