package com.sequenceiq.freeipa.flow.freeipa.trust.setup.action;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.TRUST_SETUP_FAILED;
import static com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent.TRUST_SETUP_FAILURE_HANDLED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupState;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.event.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResult;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResultConverter;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Component("FreeIpaTrustSetupFailedAction")
public class FreeIpaTrustSetupFailedAction extends FreeIpaTrustSetupBaseAction<FreeIpaTrustSetupFailureEvent> {

    @Inject
    private OperationService operationService;

    @Inject
    private TaskResultConverter taskResultConverter;

    protected FreeIpaTrustSetupFailedAction() {
        super(FreeIpaTrustSetupFailureEvent.class);
    }

    @Override
    protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaTrustSetupState,
            FreeIpaTrustSetupFlowEvent> stateContext, FreeIpaTrustSetupFailureEvent payload) {
        Flow flow = getFlow(flowParameters.getFlowId());
        flow.setFlowFailed(payload.getException());
        return super.createFlowContext(flowParameters, stateContext, payload);
    }

    @Override
    protected void doExecute(StackContext context, FreeIpaTrustSetupFailureEvent payload, Map<Object, Object> variables) throws Exception {
        Stack stack = context.getStack();
        String statusReason = "Failed to prepare cross-realm trust FreeIPA: " + getErrorReason(payload);
        updateStatuses(context.getStack(), TRUST_SETUP_FAILED, statusReason, TrustStatus.TRUST_SETUP_FAILED);
        operationService.failOperation(stack.getAccountId(), getOperationId(variables), statusReason,
                taskResultConverter.convertSuccessfulTasks(payload.getTaskResults().getSuccessfulTasks(), stack.getEnvironmentCrn()),
                getFailureDetails(payload, stack));
        sendEvent(context, new StackEvent(TRUST_SETUP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
    }

    private List<FailureDetails> getFailureDetails(FreeIpaTrustSetupFailureEvent payload, Stack stack) {
        if (payload.getTaskResults().hasErrors()) {
            return taskResultConverter.convertFailedTasks(payload.getTaskResults().getErrors(), stack.getEnvironmentCrn());
        } else {
            return List.of(taskResultConverter.convertFailedTaskResult(payload.getException().getMessage(), stack.getEnvironmentCrn()));
        }
    }

    protected String getErrorReason(FreeIpaTrustSetupFailureEvent payload) {
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
