package com.sequenceiq.environment.environment.flow.config.update;

import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.FINALIZE_ENV_STACK_CONIFG_UPDATES_EVENT;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesFailedEvent;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

@Configuration
public class EnvStackConfigUpdatesActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvStackConfigUpdatesActions.class);

    private final EnvironmentService environmentService;

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvStackConfigUpdatesActions(EnvironmentService environmentService,
        EnvironmentStatusUpdateService environmentStatusUpdateService,
        EnvironmentMetricService metricService) {
        this.environmentService = environmentService;
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "STACK_CONFIG_UPDATES_START_STATE")
    public Action<?, ?> collectClusterInfo() {
        return new AbstractEnvStackConfigUpdatesAction<>(EnvStackConfigUpdatesEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvStackConfigUpdatesEvent payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                    .updateEnvironmentStatusAndNotify(context, payload,
                        getCurrentStatus(payload.getResourceId()),
                        ResourceEvent.ENVIRONMENT_STACK_CONFIGS_UPDATE_STARTED,
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE);

                sendEvent(context,
                    EnvStackConfigUpdatesHandlerSelectors.STACK_CONFIG_UPDATES_HANDLER_EVENT
                        .selector(), environmentDto);
            }
        };
    }

    @Bean(name = "STACK_CONFIG_UPDATES_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvStackConfigUpdatesAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                EnvironmentDto environmentDto = environmentStatusUpdateService
                    .updateEnvironmentStatusAndNotify(context, payload,
                        getCurrentStatus(payload.getResourceId()),
                        ResourceEvent.ENVIRONMENT_STACK_CONFIGS_UPDATE_FINISHED,
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FINISHED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_STACK_CONFIG_UPDATE_FINISHED,
                    environmentDto);
                LOGGER.info("Flow entered into STACK_CONFIG_UPDATES_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_STACK_CONIFG_UPDATES_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "STACK_CONFIG_UPDATES_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvStackConfigUpdatesAction<>(EnvStackConfigUpdatesFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvStackConfigUpdatesFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn(
                    String.format("Failed to update environments stack configs '%s'. Status: '%s'.",
                        payload.getEnvironmentDto(), payload.getEnvironmentStatus()),
                    payload.getException());
                EnvironmentDto environmentDto = environmentStatusUpdateService
                    .updateFailedEnvironmentStatusAndNotify(context, payload,
                        getCurrentStatus(payload.getResourceId()),
                        ResourceEvent.ENVIRONMENT_STACK_CONFIGS_UPDATE_FAILED,
                        EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FAILED_STATE);
                metricService.incrementMetricCounter(MetricType.ENV_STACK_CONFIG_UPDATE_FAILED,
                    environmentDto,
                    payload.getException());
                sendEvent(context, HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT.event(), payload);
            }
        };
    }

    private EnvironmentStatus getCurrentStatus(Long envId) {
        return environmentService
            .findEnvironmentById(envId)
            .map(Environment::getStatus)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Cannot get status of environment, because it does not exist: %s. ",
                    envId)
            ));
    }
}
