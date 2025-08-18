package com.sequenceiq.environment.environment.flow.hybrid.repair.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_REPAIR_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_REPAIR_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_REPAIR_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_REPAIR_TRUST_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_REPAIR_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_REPAIR_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState.TRUST_REPAIR_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairHandlerSelectors.TRUST_REPAIR_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairHandlerSelectors.TRUST_REPAIR_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.FINALIZE_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors.HANDLED_FAILED_TRUST_REPAIR_EVENT;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_REPAIR_FAILED;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_REPAIR_FINISHED;

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
import com.sequenceiq.environment.environment.flow.hybrid.repair.EnvironmentCrossRealmTrustRepairState;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairEvent;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairFailedEvent;
import com.sequenceiq.environment.environment.flow.hybrid.repair.event.EnvironmentCrossRealmTrustRepairStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvironmentCrossRealmTrustRepairActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustRepairActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvironmentCrossRealmTrustRepairActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "TRUST_REPAIR_VALIDATION_STATE")
    public Action<?, ?> crossRealmTrustRepairValidationAction() {
        return new AbstractEnvironmentCrossRealmTrustRepairAction<>(EnvironmentCrossRealmTrustRepairEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustRepairEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_REPAIR_VALIDATION_IN_PROGRESS,
                                ENVIRONMENT_REPAIR_TRUST_VALIDATION_STARTED,
                                TRUST_REPAIR_VALIDATION_STATE);
                sendEvent(context, TRUST_REPAIR_VALIDATION_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_REPAIR_STATE")
    public Action<?, ?> crossRealmTrustRepairInFreeIpaAction() {
        return new AbstractEnvironmentCrossRealmTrustRepairAction<>(EnvironmentCrossRealmTrustRepairEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustRepairEvent payload, Map<Object, Object> variables) {
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_REPAIR_IN_PROGRESS,
                                ENVIRONMENT_REPAIR_TRUST_STARTED,
                                TRUST_REPAIR_STATE);
                sendEvent(context, TRUST_REPAIR_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_REPAIR_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCrossRealmTrustRepairAction<>(EnvironmentCrossRealmTrustRepairEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustRepairEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                AVAILABLE,
                                ENVIRONMENT_REPAIR_TRUST_FINISHED,
                                TRUST_REPAIR_FINISHED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_REPAIR_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_TRUST_REPAIR_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "TRUST_REPAIR_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCrossRealmTrustRepairAction<>(EnvironmentCrossRealmTrustRepairFailedEvent.class) {

            @Override
            protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvironmentCrossRealmTrustRepairState,
                    EnvironmentCrossRealmTrustRepairStateSelectors> stateContext, EnvironmentCrossRealmTrustRepairFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustRepairFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to repair Cross Realm Trust in environment status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                TRUST_REPAIR_FAILED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_REPAIR_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_TRUST_REPAIR_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                return switch (status) {
                    case TRUST_REPAIR_VALIDATION_FAILED -> ENVIRONMENT_REPAIR_TRUST_VALIDATION_FAILED;
                    default -> ENVIRONMENT_REPAIR_TRUST_FAILED;
                };
            }
        };
    }

}
