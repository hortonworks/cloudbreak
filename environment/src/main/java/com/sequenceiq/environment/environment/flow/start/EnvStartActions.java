package com.sequenceiq.environment.environment.flow.start;

import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.FINALIZE_ENV_START_EVENT;
import static com.sequenceiq.environment.environment.flow.start.event.EnvStartStateSelectors.HANDLED_FAILED_ENV_START_EVENT;

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
import com.sequenceiq.environment.environment.flow.start.event.EnvStartEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartFailedEvent;
import com.sequenceiq.environment.environment.flow.start.event.EnvStartHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvStartActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvStartActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public EnvStartActions(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Bean(name = "START_FREEIPA_STATE")
    public Action<?, ?> startFreeipa() {
        return new AbstractEnvStartAction<>(EnvStartEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStartEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.START_FREEIPA_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_START_FREEIPA_STARTED;
                EnvStartState envStartState = EnvStartState.START_FREEIPA_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStartState);
                sendEvent(context, EnvStartHandlerSelectors.START_FREEIPA_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "START_DATALAKE_STATE")
    public Action<?, ?> startDatalake() {
        return new AbstractEnvStartAction<>(EnvStartEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStartEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.START_DATALAKE_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_START_DATALAKE_STARTED;
                EnvStartState envStartState = EnvStartState.START_DATALAKE_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStartState);
                sendEvent(context, EnvStartHandlerSelectors.START_DATALAKE_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "START_DATAHUB_STATE")
    public Action<?, ?> startDatahub() {
        return new AbstractEnvStartAction<>(EnvStartEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvStartEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.START_DATAHUB_STARTED;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_START_DATAHUB_STARTED;
                EnvStartState envStartState = EnvStartState.START_DATAHUB_STATE;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        envStartState);
                sendEvent(context, EnvStartHandlerSelectors.START_DATAHUB_HANDLER_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_START_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvStartAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_STARTED, EnvStartState.ENV_START_FINISHED_STATE);
                LOGGER.info("Flow entered into ENV_START_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_START_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_START_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvStartAction<>(EnvStartFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvStartFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to start environment", payload.getException());
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                        convertStatus(payload.getEnvironmentStatus()), EnvStartState.ENV_START_FAILED_STATE);
                sendEvent(context, HANDLED_FAILED_ENV_START_EVENT.event(), payload);
            }
        };
    }

    private ResourceEvent convertStatus(EnvironmentStatus status) {
        switch (status) {
            case START_FREEIPA_FAILED:
                return ResourceEvent.ENVIRONMENT_START_FREEIPA_FAILED;
            case START_DATALAKE_FAILED:
                return ResourceEvent.ENVIRONMENT_START_DATALAKE_FAILED;
            case START_DATAHUB_FAILED:
                return ResourceEvent.ENVIRONMENT_START_DATAHUB_FAILED;
            default:
                return ResourceEvent.ENVIRONMENT_START_FAILED;
        }
    }
}
