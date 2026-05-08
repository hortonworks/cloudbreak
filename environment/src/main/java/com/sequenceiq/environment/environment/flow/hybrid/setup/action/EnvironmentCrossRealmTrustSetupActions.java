package com.sequenceiq.environment.environment.flow.hybrid.setup.action;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED;
import static com.sequenceiq.common.api.type.EnvironmentType.HYBRID;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_VALIDATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.EnvironmentCrossRealmTrustSetupState.TRUST_SETUP_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupHandlerSelectors.TRUST_SETUP_VALIDATION_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.FINALIZE_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupStateSelectors.HANDLED_FAILED_TRUST_SETUP_EVENT;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_SETUP_FAILED;
import static com.sequenceiq.environment.metrics.MetricType.ENV_TRUST_SETUP_FINISHED;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setup.event.EnvironmentCrossRealmTrustSetupFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvironmentCrossRealmTrustSetupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    private final EnvironmentService environmentService;

    public EnvironmentCrossRealmTrustSetupActions(
            EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService,
            EnvironmentService environmentService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
        this.environmentService = environmentService;
    }

    @Bean(name = "TRUST_SETUP_VALIDATION_STATE")
    public Action<?, ?> crossRealmTrustSetupValidationAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupEvent payload, Map<Object, Object> variables) {
                if (payload != null && payload.getRemoteEnvironmentCrn() != null) {
                    LOGGER.info("Remote environment CRN is set in request: {}, updating it in the environment: {}",
                            payload.getRemoteEnvironmentCrn(), payload.getResourceCrn());
                    environmentService.updateRemoteEnvironmentCrn(
                            payload.getAccountId(),
                            payload.getResourceCrn(),
                            payload.getRemoteEnvironmentCrn());
                }

                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_SETUP_VALIDATION_IN_PROGRESS,
                                ENVIRONMENT_SETUP_TRUST_VALIDATION_STARTED,
                                TRUST_SETUP_VALIDATION_STATE);
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
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                TRUST_SETUP_IN_PROGRESS,
                                ENVIRONMENT_SETUP_TRUST_STARTED,
                                TRUST_SETUP_STATE);
                sendEvent(context, TRUST_SETUP_HANDLER.selector(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupEvent payload, Map<Object, Object> variables) {
                boolean hybrid = environmentService.findById(payload.getResourceId())
                        .map(env -> HYBRID.equals(env.getEnvironmentType()))
                        .orElse(false);
                EnvironmentStatus targetStatus = hybrid ? TRUST_SETUP_FINISH_REQUIRED : AVAILABLE;
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(
                                context,
                                payload,
                                targetStatus,
                                ENVIRONMENT_SETUP_TRUST_FINISHED,
                                TRUST_SETUP_FINISHED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_SETUP_FINISHED, environmentDto);
                sendEvent(context, FINALIZE_TRUST_SETUP_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "TRUST_SETUP_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCrossRealmTrustSetupAction<>(EnvironmentCrossRealmTrustSetupFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvironmentCrossRealmTrustSetupFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error(String.format("Failed to setup Cross Realm Trust in environment status: '%s'.",
                        payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(
                                context,
                                payload,
                                payload.getEnvironmentStatus(),
                                convertStatus(payload.getEnvironmentStatus()),
                                TRUST_SETUP_FAILED_STATE);
                metricService.incrementMetricCounter(ENV_TRUST_SETUP_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_TRUST_SETUP_EVENT.event(), payload);
            }

            private ResourceEvent convertStatus(EnvironmentStatus status) {
                return switch (status) {
                    case TRUST_SETUP_VALIDATION_FAILED -> ENVIRONMENT_SETUP_TRUST_VALIDATION_FAILED;
                    default -> ENVIRONMENT_SETUP_TRUST_FAILED;
                };
            }
        };
    }

}
