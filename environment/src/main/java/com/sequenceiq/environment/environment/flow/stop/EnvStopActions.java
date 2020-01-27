package com.sequenceiq.environment.environment.flow.stop;

import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FINALIZE_ENV_STOP_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.HANDLED_FAILED_ENV_STOP_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopFailedEvent;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvStopActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvStopActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvStopActions(EnvironmentStatusUpdateService environmentStatusUpdateService, EnvironmentMetricService metricService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "STOP_DATAHUB_STATE")
    public Action<?, ?> stopDatahub() {
        return new AbstractEnvStopAction<>(EnvStopEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStopEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.STOP_DATAHUB_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_STOP_DATAHUB_STARTED;
                EnvStopState envStopState = EnvStopState.STOP_DATAHUB_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStopState);
                sendEvent(context, EnvStopHandlerSelectors.STOP_DATAHUB_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "STOP_DATALAKE_STATE")
    public Action<?, ?> stopDatalake() {
        return new AbstractEnvStopAction<>(EnvStopEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStopEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.STOP_DATALAKE_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_STOP_DATALAKE_STARTED;
                EnvStopState envStopState = EnvStopState.STOP_DATALAKE_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStopState);
                sendEvent(context, EnvStopHandlerSelectors.STOP_DATALAKE_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "STOP_FREEIPA_STATE")
    public Action<?, ?> stopFreeipa() {
        return new AbstractEnvStopAction<>(EnvStopEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStopEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.STOP_FREEIPA_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_STOP_FREEIPA_STARTED;
                EnvStopState envStopState = EnvStopState.STOP_FREEIPA_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStopState);
                sendEvent(context, EnvStopHandlerSelectors.STOP_FREEIPA_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_STOP_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvStopAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.ENV_STOPPED,
                        ResourceEvent.ENVIRONMENT_STOPPED, EnvStopState.ENV_STOP_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_STOP_FINISHED, environmentDto);
                LOGGER.info("Flow entered into ENV_STOP_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_STOP_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_STOP_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvStopAction<>(EnvStopFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvStopFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn(String.format("Failed to stop environment '%s'. Status: '%s'.",
                        payload.getEnvironmentDto(), payload.getEnvironmentStatus()), payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                        convertStatus(payload.getEnvironmentStatus()), EnvStopState.ENV_STOP_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_STOP_FAILED, environmentDto, payload.getException());
                sendEvent(context, HANDLED_FAILED_ENV_STOP_EVENT.event(), payload);
            }
        };
    }

    private ResourceEvent convertStatus(EnvironmentStatus status) {
        switch (status) {
            case STOP_FREEIPA_FAILED:
                return ResourceEvent.ENVIRONMENT_STOP_FREEIPA_FAILED;
            case STOP_DATALAKE_FAILED:
                return ResourceEvent.ENVIRONMENT_STOP_DATALAKE_FAILED;
            case STOP_DATAHUB_FAILED:
                return ResourceEvent.ENVIRONMENT_STOP_DATAHUB_FAILED;
            default:
                return ResourceEvent.ENVIRONMENT_STOP_FAILED;
        }
    }
}
