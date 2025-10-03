package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_COMPUTE_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATAHUB_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATALAKE_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_EXPERIENCE_EVENT;

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
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClusterDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvClustersDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvClustersDeleteActions.class);

    private final EnvironmentService environmentService;

    private final EventSenderService eventService;

    private final EnvironmentMetricService metricService;

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public EnvClustersDeleteActions(EnvironmentService environmentService, EventSenderService eventService, EnvironmentMetricService metricService,
            EnvironmentStatusUpdateService environmentStatusUpdateService) {

        this.environmentService = environmentService;
        this.eventService = eventService;
        this.metricService = metricService;
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Bean(name = "DATAHUB_CLUSTERS_DELETE_STARTED_STATE")
    public Action<?, ?> datahubClustersDeleteAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_DATAHUB_CLUSTERS_DELETION_STARTED;
                EnvClustersDeleteState envClustersDeleteState = EnvClustersDeleteState.DATAHUB_CLUSTERS_DELETE_STARTED_STATE;
                String logDeleteState = "Data Hub clusters";

                EnvironmentDeletionDto envDto = commonUpdateEnvironmentAndNotify(context, payload, environmentStatus, resourceEvent,
                        envClustersDeleteState, logDeleteState);
                sendEvent(context, DELETE_DATAHUB_CLUSTERS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "EXPERIENCE_DELETE_STARTED_STATE")
    public Action<?, ?> experienceDeleteAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.EXPERIENCE_DELETE_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_EXPERIENCE_DELETION_STARTED,
                        EnvClustersDeleteState.EXPERIENCE_DELETE_STARTED_STATE);
                EnvironmentDeletionDto environmentDeletionDto = EnvironmentDeletionDto.builder()
                        .withEnvironmentDto(envDto)
                        .withForceDelete(payload.isForceDelete())
                        .withId(payload.getResourceId())
                        .build();
                sendEvent(context, DELETE_EXPERIENCE_EVENT.selector(), environmentDeletionDto);
            }
        };
    }

    @Bean(name = "DATALAKE_CLUSTERS_DELETE_STARTED_STATE")
    public Action<?, ?> datalakeClustersDeleteAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                boolean hybridEnvironment = environmentService.isHybridEnvironment(payload.getResourceId());
                EnvironmentDeletionDto envDto;
                if (hybridEnvironment) {
                    envDto = convertToDto(payload);
                } else {
                    EnvironmentStatus environmentStatus = EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS;
                    ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED;
                    EnvClustersDeleteState envClustersDeleteState = EnvClustersDeleteState.DATALAKE_CLUSTERS_DELETE_STARTED_STATE;
                    String logDeleteState = "Data Lake clusters";

                    envDto = commonUpdateEnvironmentAndNotify(context, payload, environmentStatus, resourceEvent,
                            envClustersDeleteState, logDeleteState);
                }
                sendEvent(context, DELETE_DATALAKE_CLUSTERS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "COMPUTE_CLUSTERS_DELETE_STARTED_STATE")
    public Action<?, ?> externalizedComputeClustersDeleteAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.COMPUTE_CLUSTERS_DELETE_IN_PROGRESS;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTERS_DELETION_STARTED;
                EnvClustersDeleteState envClustersDeleteState = EnvClustersDeleteState.COMPUTE_CLUSTERS_DELETE_STARTED_STATE;
                String logDeleteState = "Compute clusters";

                EnvironmentDeletionDto envDto = commonUpdateEnvironmentAndNotify(context, payload, environmentStatus, resourceEvent,
                        envClustersDeleteState, logDeleteState);
                sendEvent(context, DELETE_COMPUTE_CLUSTERS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_CLUSTERS_DELETE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvClusterDeleteFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvClusterDeleteFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to delete environment", payload.getException());
                Exception e = payload.getException();
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            environment.setStatusReason(payload.getMessage() == null ? e.getMessage() : payload.getMessage());
                            environment.setStatus(EnvironmentStatus.DELETE_FAILED);
                            Environment result = environmentService.save(environment);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(result);
                            metricService.incrementMetricCounter(MetricType.ENV_CLUSTERS_DELETION_FAILED, environmentDto, payload.getException());
                            String errorMessage = "Unknown error";
                            if (payload.getException() != null && payload.getException().getMessage() != null) {
                                errorMessage = payload.getException().getMessage();
                            }
                            eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(),
                                    ResourceEvent.ENVIRONMENT_DELETION_FAILED_WITH_REASON, List.of(errorMessage));

                        }, () -> LOGGER.error("Cannot set delete failed to env because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_CLUSTERS_DELETE_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT.event(), payload);
            }

            @Override
            protected CommonContext createFlowContext(
                    FlowParameters flowParameters, StateContext<EnvClustersDeleteState, EnvClustersDeleteStateSelectors> stateContext,
                    EnvClusterDeleteFailedEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return new CommonContext(flowParameters);
            }
        };
    }

    private EnvironmentDeletionDto commonUpdateEnvironmentAndNotify(CommonContext context, EnvDeleteEvent payload,
        EnvironmentStatus environmentStatus, ResourceEvent resourceEvent,
        EnvClustersDeleteState envClustersDeleteState, String logDeleteState) {

        environmentService
                .findEnvironmentById(payload.getResourceId())
                .ifPresentOrElse(environment -> {
                    environment.setStatus(environmentStatus);
                    environment.setStatusReason(null);
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), resourceEvent);
                }, () -> LOGGER.error("Cannot delete {} because the environment does not exist: {}. "
                        + "But the flow will continue, how can this happen?", logDeleteState, payload.getResourceId()));
        LOGGER.info("Flow entered into {}", envClustersDeleteState.name());
        return convertToDto(payload);
    }

    private EnvironmentDeletionDto convertToDto(EnvDeleteEvent payload) {
        EnvironmentDto envDto = new EnvironmentDto();
        envDto.setId(payload.getResourceId());
        envDto.setResourceCrn(payload.getResourceCrn());
        envDto.setName(payload.getResourceName());

        EnvironmentDeletionDto environmentDeletionDto = EnvironmentDeletionDto.builder()
                .withEnvironmentDto(envDto)
                .withForceDelete(payload.isForceDelete())
                .withId(payload.getResourceId())
                .build();

        return environmentDeletionDto;
    }

}
