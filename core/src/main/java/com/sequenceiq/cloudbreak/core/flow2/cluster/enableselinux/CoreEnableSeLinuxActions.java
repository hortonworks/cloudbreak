package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CHANGED_SELINUX;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_CHANGING_SELINUX;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_FAILED_SELINUX;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_VALIDATING_SELINUX;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreEnableSeLinuxStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event.CoreValidateEnableSeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class CoreEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreEnableSeLinuxActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "ENABLE_SELINUX_CORE_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.debug("Validating payload for SELinux Mode change {}", payload);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_IN_PROGRESS,
                        "Starting to validate stack for setting SELinux to 'ENFORCING'.");
                flowMessageService.fireEventAndLog(stack.getId(),
                        STACK_VALIDATING_SELINUX.name(),
                        STACK_VALIDATING_SELINUX,
                        String.valueOf(payload.getResourceId()));
                CoreValidateEnableSeLinuxHandlerEvent validationEvent = new CoreValidateEnableSeLinuxHandlerEvent(stack.getId());
                sendEvent(context, validationEvent);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_STATE")
    public Action<?, ?> enableSeLinuxAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating SELinux Mode {}", payload);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_CHANGING_SELINUX.name(),
                        STACK_CHANGING_SELINUX,
                        String.valueOf(payload.getResourceId()));
                CoreEnableSeLinuxHandlerEvent enableSeLinuxEvent = new CoreEnableSeLinuxHandlerEvent(context.getStack().getId());
                sendEvent(context, enableSeLinuxEvent);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.debug("Updated SELinux Mode to 'ENFORCING' for stack - {}", payload);
                getMetricService().incrementMetricCounter(MetricType.ENABLE_SELINUX_SUCCESSFUL, stack.getDisplayName(), stack.getResourceCrn());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_COMPLETE, "Updated SELinux mode to 'ENFORCING'.");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_CHANGED_SELINUX.name(),
                        STACK_CHANGED_SELINUX,
                        String.valueOf(payload.getResourceId()));
                String selector = FINALIZE_ENABLE_SELINUX_CORE_EVENT.selector();
                CoreEnableSeLinuxEvent finalizeEvent = new CoreEnableSeLinuxEvent(selector, stack.getId());
                sendEvent(context, selector, finalizeEvent);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_CORE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractCoreEnableSeLinuxAction<>(CoreEnableSeLinuxFailedEvent.class) {

            @Override
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<CoreEnableSeLinuxState,
                CoreEnableSeLinuxStateSelectors> stateContext, CoreEnableSeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, CoreEnableSeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Enable SELinux on Stack '%s'.", payload.getResourceId()), payload.getException());
                StackDtoDelegate stack = context.getStack();
                getMetricService().incrementMetricCounter(MetricType.ENABLE_SELINUX_FAILED, stack.getResourceCrn(), stack.getDisplayName(),
                        "" + stack.getId(), "Exception: " + payload.getException().getMessage());
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_FAILED, errorReason);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_FAILED_SELINUX.name(),
                        STACK_FAILED_SELINUX,
                        String.valueOf(payload.getResourceId()));
                CoreEnableSeLinuxFailedEvent failHandledPayload = new CoreEnableSeLinuxFailedEvent(HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT.event(),
                        payload.getResourceId(), payload.getFailedPhase(), payload.getException());
                sendEvent(context, HANDLED_FAILED_ENABLE_SELINUX_CORE_EVENT.event(), failHandledPayload);
            }

            private String getErrorReason(Exception payloadException) {
                return payloadException == null || payloadException.getMessage() == null ? "Failed to update SELinux mode to 'ENFORCING'. Detail: " +
                        "Unknown error" : payloadException.getMessage();
            }
        };
    }

}
