package com.sequenceiq.environment.environment.flow.enableselinux.freeipa;

import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_FREEIPA_HANDLER;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxHandlerSelectors.ENABLE_SELINUX_FREEIPA_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors.HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxFailedEvent;
import com.sequenceiq.environment.environment.flow.enableselinux.freeipa.event.EnvironmentEnableSeLinuxStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvironmentEnableSeLinuxActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentEnableSeLinuxActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    private final FreeIpaService freeIpaService;

    public EnvironmentEnableSeLinuxActions(EnvironmentStatusUpdateService environmentStatusUpdateService,
        EnvironmentMetricService metricService, FreeIpaService freeIpaService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
        this.freeIpaService = freeIpaService;
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_VALIDATION_STATE")
    public Action<?, ?> enableSeLinuxValidationAction() {
        return new AbstractEnvironmentEnableSeLinuxAction<>(EnvironmentEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.ENABLE_SELINUX_VALIDATION_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_VALIDATION_STARTED,
                                EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_VALIDATION_STATE);
                sendEvent(context, ENABLE_SELINUX_FREEIPA_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_STATE")
    public Action<?, ?> enableSeLinuxInFreeIpaAction() {
        return new AbstractEnvironmentEnableSeLinuxAction<>(EnvironmentEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.ENABLE_SELINUX_ON_FREEIPA_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_ON_FREEIPA_STARTED,
                                EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_STATE);
                sendEvent(context, ENABLE_SELINUX_FREEIPA_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentEnableSeLinuxAction<>(EnvironmentEnableSeLinuxEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentEnableSeLinuxEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.ENV_STOPPED,
                                ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_FINISHED,
                                EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_ENABLE_SELINUX_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_ENABLE_SELINUX_FREEIPA_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENABLE_SELINUX_FREEIPA_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentEnableSeLinuxAction<>(EnvironmentEnableSeLinuxFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvironmentEnableSeLinuxState,
                    EnvironmentEnableSeLinuxStateSelectors> stateContext, EnvironmentEnableSeLinuxFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentEnableSeLinuxFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to Enable SeLinux in environment '%s'. Status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                List.of(payload.getException().getMessage()),
                                EnvironmentEnableSeLinuxState.ENABLE_SELINUX_FREEIPA_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_ENABLE_SELINUX_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_ENABLE_SELINUX_FREEIPA_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                switch (status) {
                    case ENABLE_SELINUX_VALIDATION_FAILED:
                        return ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_VALIDATION_FAILED;
                    case ENABLE_SELINUX_ON_FREEIPA_FAILED:
                        return ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_ON_FREEIPA_FAILED;
                    default:
                        return ResourceEvent.ENVIRONMENT_ENABLE_SELINUX_FAILED;
                }
            }
        };
    }

}
