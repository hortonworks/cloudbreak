package com.sequenceiq.environment.environment.flow.hybrid.cancel.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_REQUIRED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState.TRUST_CANCEL_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINALIZE_TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.HANDLED_FAILED_TRUST_CANCEL_EVENT;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_CANCEL_FAILED;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_CANCEL_FINISHED;

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
import com.sequenceiq.environment.environment.flow.hybrid.cancel.EnvironmentCrossRealmTrustCancelState;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvironmentCrossRealmTrustCancelActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustCancelActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvironmentCrossRealmTrustCancelActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "TRUST_CANCEL_VALIDATION_STATE")
    public Action<?, ?> crossRealmTrustCancelValidationAction() {
        return new AbstractEnvironmentCrossRealmTrustCancelAction<>(EnvironmentCrossRealmTrustCancelEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustCancelEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_CANCEL_VALIDATION_IN_PROGRESS,
                                ENVIRONMENT_CANCEL_TRUST_VALIDATION_STARTED,
                                TRUST_CANCEL_VALIDATION_STATE);
                sendEvent(context, TRUST_CANCEL_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_CANCEL_STATE")
    public Action<?, ?> crossRealmTrustCancelInFreeIpaAction() {
        return new AbstractEnvironmentCrossRealmTrustCancelAction<>(EnvironmentCrossRealmTrustCancelEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustCancelEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_CANCEL_IN_PROGRESS,
                                ENVIRONMENT_CANCEL_TRUST_STARTED,
                                TRUST_CANCEL_STATE);
                sendEvent(context, TRUST_CANCEL_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_CANCEL_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCrossRealmTrustCancelAction<>(EnvironmentCrossRealmTrustCancelEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustCancelEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_SETUP_REQUIRED,
                                ENVIRONMENT_CANCEL_TRUST_FINISHED,
                                TRUST_CANCEL_FINISHED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_CANCEL_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_TRUST_CANCEL_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "TRUST_CANCEL_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCrossRealmTrustCancelAction<>(EnvironmentCrossRealmTrustCancelFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvironmentCrossRealmTrustCancelState,
                    EnvironmentCrossRealmTrustCancelStateSelectors> stateContext, EnvironmentCrossRealmTrustCancelFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustCancelFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to cancel Cross Realm Trust in environment status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                TRUST_CANCEL_FAILED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_CANCEL_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_TRUST_CANCEL_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                return switch (status) {
                    case TRUST_CANCEL_VALIDATION_FAILED -> ENVIRONMENT_CANCEL_TRUST_VALIDATION_FAILED;
                    default -> ENVIRONMENT_CANCEL_TRUST_FAILED;
                };
            }
        };
    }

}
