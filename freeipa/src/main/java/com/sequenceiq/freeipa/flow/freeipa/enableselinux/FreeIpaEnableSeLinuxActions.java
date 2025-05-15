package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaEnableSeLinuxStateSelectors;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaValidateEnableSeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class FreeIpaEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaEnableSeLinuxActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Bean(name = "ENABLE_SELINUX_FREEIPA_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractFreeIpaEnableSeLinuxAction<>(FreeIpaEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.debug("Validation for SELinux enablement for stack payload - {}", payload);
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPDATE_IN_PROGRESS, "Starting to validate SELinux mode change.");
                FreeIpaValidateEnableSeLinuxHandlerEvent validationHandlerPayload =
                        new FreeIpaValidateEnableSeLinuxHandlerEvent(payload.getResourceId(), payload.getOperationId());
                sendEvent(context, validationHandlerPayload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_STATE")
    public Action<?, ?> enableSeLinuxInFreeIpaAction() {
        return new AbstractFreeIpaEnableSeLinuxAction<>(FreeIpaEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating SELinux mode to 'ENFORCED' for stack crn - {}", context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_IN_PROGRESS, "Starting to modify SELinux mode change.");
                FreeIpaEnableSeLinuxHandlerEvent enableSeLinuxHandlerPayload =
                        new FreeIpaEnableSeLinuxHandlerEvent(payload.getResourceId(), payload.getOperationId());
                sendEvent(context, enableSeLinuxHandlerPayload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractFreeIpaEnableSeLinuxAction<>(FreeIpaEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Finished setting SELinux mode to 'ENFORCED' for stack crn - {}", context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_COMPLETE, "Finished setting SELinux mode to " +
                                        "'ENFORCING' for stack " + context.getStack().getResourceCrn());
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                operationService.completeOperation(stack.getAccountId(), payload.getOperationId(), Set.of(successDetails), Set.of());
                String selector = FreeIpaEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT.event();
                FreeIpaEnableSeLinuxEvent finalizeEvent =
                        new FreeIpaEnableSeLinuxEvent(selector, payload.getResourceId(), payload.getOperationId());
                sendEvent(context, FreeIpaEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT.event(), finalizeEvent);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractFreeIpaEnableSeLinuxAction<>(FreeIpaEnableSeLinuxFailedEvent.class) {
            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaEnableSeLinuxState,
                    FreeIpaEnableSeLinuxStateSelectors> stateContext, FreeIpaEnableSeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, FreeIpaEnableSeLinuxFailedEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String message = "Setting SELinux to 'ENFORCING' failed during " + payload.getFailedPhase();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(), List.of());
                enableStatusChecker(stack, "Failed to update SELinux for FreeIPA.");
                sendEvent(context, HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT.event(), payload);
            }
        };
    }
}
