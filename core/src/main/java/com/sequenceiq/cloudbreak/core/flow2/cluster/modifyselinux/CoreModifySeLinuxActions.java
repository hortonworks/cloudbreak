package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux;

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
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;

@Configuration
public class CoreModifySeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreModifySeLinuxActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "MODIFY_SELINUX_CORE_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractCoreModifySeLinuxAction<>(CoreModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreModifySeLinuxEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.debug("Validating payload for SELinux Mode change {}", payload);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_IN_PROGRESS,
                        "Starting to validate stack for setting SELinux to 'ENFORCING'.");
                flowMessageService.fireEventAndLog(stack.getId(),
                        STACK_VALIDATING_SELINUX.name(),
                        STACK_VALIDATING_SELINUX,
                        stack.getType().getResourceType(),
                        String.valueOf(payload.getResourceId()));
                CoreValidateModifySeLinuxHandlerEvent validationEvent = new CoreValidateModifySeLinuxHandlerEvent(stack.getId(), payload.getSelinuxMode());
                sendEvent(context, validationEvent);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_CORE_STATE")
    public Action<?, ?> enableSeLinuxAction() {
        return new AbstractCoreModifySeLinuxAction<>(CoreModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreModifySeLinuxEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Updating SELinux Mode {}", payload);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_CHANGING_SELINUX.name(),
                        STACK_CHANGING_SELINUX,
                        context.getStack().getType().getResourceType(),
                        String.valueOf(payload.getResourceId()));
                CoreModifySeLinuxHandlerEvent enableSeLinuxEvent = new CoreModifySeLinuxHandlerEvent(context.getStack().getId(), payload.getSelinuxMode());
                sendEvent(context, enableSeLinuxEvent);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_CORE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractCoreModifySeLinuxAction<>(CoreModifySeLinuxEvent.class) {
            @Override
            protected void doExecute(StackContext context, CoreModifySeLinuxEvent payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                LOGGER.debug("Updated SELinux Mode to {} for stack - {}", payload.getSelinuxMode(), payload);
                getMetricService().incrementMetricCounter(MetricType.MODIFY_SELINUX_SUCCESSFUL, stack.getDisplayName(), stack.getResourceCrn());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_COMPLETE,
                        "Updated SELinux mode to " + payload.getSelinuxMode() + ".");
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_CHANGED_SELINUX.name(),
                        STACK_CHANGED_SELINUX,
                        stack.getType().getResourceType(),
                        String.valueOf(payload.getResourceId()));
                String selector = CoreModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_CORE_EVENT.selector();
                CoreModifySeLinuxEvent finalizeEvent = new CoreModifySeLinuxEvent(selector, stack.getId(), payload.getSelinuxMode());
                sendEvent(context, selector, finalizeEvent);
            }
        };
    }

    @Bean(name = "MODIFY_SELINUX_CORE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractCoreModifySeLinuxAction<>(CoreModifySeLinuxFailedEvent.class) {

            @Override
            protected void doExecute(StackContext context, CoreModifySeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Modify SELinux on Stack '%s'.", payload.getResourceId()), payload.getException());
                StackDtoDelegate stack = context.getStack();
                getMetricService().incrementMetricCounter(MetricType.MODIFY_SELINUX_FAILED, stack.getResourceCrn(), stack.getDisplayName(),
                        "" + stack.getId(), "Exception: " + payload.getException().getMessage());
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.SELINUX_MODE_UPDATE_FAILED, errorReason);
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        STACK_FAILED_SELINUX.name(),
                        STACK_FAILED_SELINUX,
                        String.valueOf(payload.getResourceId()));
                CoreModifySeLinuxFailedEvent failHandledPayload = new CoreModifySeLinuxFailedEvent(
                        CoreModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT.event(),
                        payload.getResourceId(), payload.getFailedPhase(), payload.getException());
                sendEvent(context, CoreModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT.event(), failHandledPayload);
            }

            private String getErrorReason(Exception payloadException) {
                return payloadException == null || payloadException.getMessage() == null ? "Failed to update SELinux mode to 'ENFORCING'. Detail: " +
                        "Unknown error" : payloadException.getMessage();
            }
        };
    }

}
