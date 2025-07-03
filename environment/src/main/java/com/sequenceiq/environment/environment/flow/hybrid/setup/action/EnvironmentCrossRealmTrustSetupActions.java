package com.sequenceiq.environment.environment.flow.hybrid.setup.action;

import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINALIZE_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.HANDLED_FAILED_TRUST_SETUP_EVENT;

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
import com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvironmentCrossRealmTrustSetupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvironmentCrossRealmTrustSetupActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "TRUST_SETUP_VALIDATION_STATE")
    public Action<?, ?> crossRealmTrustSetupValidationAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                EnvironmentStatus.TRUST_SETUP_VALIDATION_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED,
                                EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_VALIDATION_STATE);
                sendEvent(context, TRUST_SETUP_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_STATE")
    public Action<?, ?> crossRealmTrustSetupInFreeIpaAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.TRUST_SETUP_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_SETUP_TRUST_STARTED,
                                EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_STATE);
                sendEvent(context, TRUST_SETUP_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED,
                                ResourceEvent.ENVIRONMENT_SETUP_TRUST_FINISHED,
                                EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_TRUST_SETUP_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_TRUST_SETUP_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvironmentCrossRealmTrustSetupState,
                    EnvironmentCrossRealmTrustSetupStateSelectors> stateContext, EnvironmentCrossRealmTrustSetupFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to setup Cross Realm Trust in environment status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_TRUST_SETUP_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_TRUST_SETUP_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                return switch (status) {
                    case TRUST_SETUP_VALIDATION_FAILED -> ResourceEvent.ENVIRONMENT_SETUP_TRUST_VALIDATION_FAILED;
                    default -> ResourceEvent.ENVIRONMENT_SETUP_TRUST_FAILED;
                };
            }
        };
    }

}
