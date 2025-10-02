package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_FINISH_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_FINISH_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_FINISH_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_FINISH_TRUST_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_FINISH_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState.TRUST_SETUP_FINISH_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINALIZE_TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.HANDLED_FAILED_TRUST_SETUP_FINISH_EVENT;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_SETUP_FINISH_FINISHED;

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
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.EnvironmentCrossRealmTrustSetupFinishState;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvironmentCrossRealmTrustSetupFinishActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupFinishActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvironmentCrossRealmTrustSetupFinishActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "TRUST_SETUP_FINISH_VALIDATION_STATE")
    public Action<?, ?> crossRealmFinishValidationAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupFinishAction<>(EnvironmentCrossRealmTrustSetupFinishEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFinishEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_SETUP_FINISH_VALIDATION_IN_PROGRESS,
                                ENVIRONMENT_SETUP_FINISH_TRUST_VALIDATION_STARTED,
                                TRUST_SETUP_FINISH_VALIDATION_STATE
                        );
                sendEvent(context, SETUP_FINISH_TRUST_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FINISH_STATE")
    public Action<?, ?> crossRealmFinishInFreeIpaAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupFinishAction<>(EnvironmentCrossRealmTrustSetupFinishEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFinishEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_SETUP_FINISH_IN_PROGRESS,
                                ENVIRONMENT_SETUP_FINISH_TRUST_STARTED,
                                TRUST_SETUP_FINISH_STATE
                        );
                sendEvent(context, SETUP_FINISH_TRUST_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FINISH_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupFinishAction<>(EnvironmentCrossRealmTrustSetupFinishEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFinishEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                AVAILABLE,
                                ENVIRONMENT_SETUP_FINISH_TRUST_FINISHED,
                                TRUST_SETUP_FINISH_FINISHED_STATE
                        );
                metricService.incrementMetricCounter(ENV_TRUST_SETUP_FINISH_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_TRUST_SETUP_FINISH_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FINISH_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupFinishAction<>(EnvironmentCrossRealmTrustSetupFinishFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvironmentCrossRealmTrustSetupFinishState,
                    EnvironmentCrossRealmTrustSetupFinishStateSelectors> stateContext, EnvironmentCrossRealmTrustSetupFinishFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFinishFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to finish cross realm in environment status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                TRUST_SETUP_FINISH_FAILED_STATE
                        );
                metricService.incrementMetricCounter(MetricType.ENV_TRUST_SETUP_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_TRUST_SETUP_FINISH_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                return switch (status) {
                    case TRUST_SETUP_FINISH_VALIDATION_FAILED -> ENVIRONMENT_SETUP_FINISH_TRUST_VALIDATION_FAILED;
                    default -> ENVIRONMENT_SETUP_FINISH_TRUST_FAILED;
                };
            }
        };
    }

}
