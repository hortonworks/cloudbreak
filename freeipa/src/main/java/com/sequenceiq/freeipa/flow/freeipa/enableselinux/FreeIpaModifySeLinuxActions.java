package com.sequenceiq.freeipa.flow.freeipa.enableselinux;

import static com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT;

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
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaModifySeLinuxStateSelectors;
import com.sequenceiq.freeipa.flow.freeipa.enableselinux.event.FreeIpaValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class FreeIpaModifySeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaModifySeLinuxActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Bean(name = "MODIFY_SELINUX_FREEIPA_VALIDATION_STATE")
    public Action<?, ?> modifySeLinuxValidationAction() {
        return new AbstractFreeIpaModifySeLinuxAction<>(FreeIpaModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaModifySeLinuxEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                LOGGER.debug("Validation for SELinux modification for stack payload - {}", payload);
                stackUpdater.updateStackStatus(stack, DetailedStackStatus.UPDATE_IN_PROGRESS, "Starting to validate SELinux mode change.");
                FreeIpaValidateModifySeLinuxHandlerEvent validationHandlerPayload =
                        new FreeIpaValidateModifySeLinuxHandlerEvent(payload.getResourceId(), payload.getOperationId(), payload.getSeLinuxMode());
                sendEvent(context, validationHandlerPayload);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_FREEIPA_STATE")
    public Action<?, ?> modifySeLinuxInFreeIpaAction() {
        return new AbstractFreeIpaModifySeLinuxAction<>(FreeIpaModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaModifySeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating SELinux mode to {} for stack crn - {}", payload.getSeLinuxMode(), context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_IN_PROGRESS, "Starting to modify SELinux mode change.");
                FreeIpaModifySeLinuxHandlerEvent enableSeLinuxHandlerPayload =
                        new FreeIpaModifySeLinuxHandlerEvent(payload.getResourceId(), payload.getOperationId(), payload.getSeLinuxMode());
                sendEvent(context, enableSeLinuxHandlerPayload);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_FREEIPA_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractFreeIpaModifySeLinuxAction<>(FreeIpaModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, FreeIpaModifySeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Finished setting SELinux mode to {} for stack crn - {}", payload.getSeLinuxMode(), context.getStack().getResourceCrn());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_COMPLETE, "Finished setting SELinux mode to " +
                                        payload.getSeLinuxMode() + " for stack " + context.getStack().getResourceCrn());
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                operationService.completeOperation(stack.getAccountId(), payload.getOperationId(), Set.of(successDetails), Set.of());
                String selector = FreeIpaModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT.event();
                FreeIpaModifySeLinuxEvent finalizeEvent =
                        new FreeIpaModifySeLinuxEvent(selector, payload.getResourceId(), payload.getOperationId(), payload.getSeLinuxMode());
                sendEvent(context, FreeIpaModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_FREEIPA_EVENT.event(), finalizeEvent);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_FREEIPA_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractFreeIpaModifySeLinuxAction<>(FreeIpaModifySeLinuxFailedEvent.class) {
            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<FreeIpaModifySeLinuxState,
                    FreeIpaModifySeLinuxStateSelectors> stateContext, FreeIpaModifySeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, FreeIpaModifySeLinuxFailedEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                String message = "Updating SELinux failed during " + payload.getFailedPhase();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), DetailedStackStatus.UPDATE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(), List.of());
                enableStatusChecker(stack, "Failed to update SELinux for FreeIPA.");
                sendEvent(context, HANDLED_FAILED_MODIFY_SELINUX_FREEIPA_EVENT.event(), payload);
            }
        };
    }
}
