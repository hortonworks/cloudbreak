package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors.HANDLED_FAILED_ENV_CLUSTERS_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATAHUB_CLUSTERS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_DATALAKE_CLUSTERS_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClusterDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvClustersDeleteStateSelectors;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.EnvironmentApiConverter;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.notification.NotificationService;

@Configuration
public class EnvClustersDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvClustersDeleteActions.class);

    private final EnvironmentService environmentService;

    private final NotificationService notificationService;

    private final EnvironmentApiConverter environmentApiConverter;

    private final EnvironmentMetricService metricService;

    public EnvClustersDeleteActions(EnvironmentService environmentService, NotificationService notificationService,
            EnvironmentApiConverter environmentApiConverter, EnvironmentMetricService metricService) {
        this.environmentService = environmentService;
        this.notificationService = notificationService;
        this.environmentApiConverter = environmentApiConverter;
        this.metricService = metricService;
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

                EnvironmentDto envDto = commonUpdateEnvironmentAndNotify(context, payload, environmentStatus, resourceEvent,
                        envClustersDeleteState, logDeleteState);
                sendEvent(context, DELETE_DATAHUB_CLUSTERS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "DATALAKE_CLUSTERS_DELETE_STARTED_STATE")
    public Action<?, ?> datalakeClustersDeleteAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentStatus environmentStatus = EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_DATALAKE_CLUSTERS_DELETION_STARTED;
                EnvClustersDeleteState envClustersDeleteState = EnvClustersDeleteState.DATALAKE_CLUSTERS_DELETE_STARTED_STATE;
                String logDeleteState = "Data Lake clustesr";

                EnvironmentDto envDto = commonUpdateEnvironmentAndNotify(context, payload, environmentStatus, resourceEvent,
                        envClustersDeleteState, logDeleteState);
                sendEvent(context, DELETE_DATALAKE_CLUSTERS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_CLUSTERS_DELETE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvClustersDeleteAction<>(EnvClusterDeleteFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvClusterDeleteFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to delete environment", payload.getException());
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            environment.setStatusReason(payload.getException().getMessage());
                            environment.setStatus(EnvironmentStatus.DELETE_FAILED);
                            Environment result = environmentService.save(environment);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(result);
                            metricService.incrementMetricCounter(MetricType.ENV_CLUSTERS_DELETION_FAILED, environmentDto, payload.getException());
                            SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                            notificationService.send(ResourceEvent.ENVIRONMENT_DELETION_FAILED, simpleResponse, context.getFlowTriggerUserCrn());
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

    private EnvironmentDto commonUpdateEnvironmentAndNotify(CommonContext context, EnvDeleteEvent payload,
            EnvironmentStatus environmentStatus, ResourceEvent resourceEvent, EnvClustersDeleteState envClustersDeleteState, String logDeleteState) {

        environmentService
                .findEnvironmentById(payload.getResourceId())
                .ifPresentOrElse(environment -> {
                    environment.setStatus(environmentStatus);
                    Environment env = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(env);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(resourceEvent, simpleResponse, context.getFlowTriggerUserCrn());
                }, () -> LOGGER.error("Cannot delete {} because the environment does not exist: {}. "
                        + "But the flow will continue, how can this happen?", logDeleteState, payload.getResourceId()));
        EnvironmentDto envDto = new EnvironmentDto();
        envDto.setId(payload.getResourceId());
        envDto.setResourceCrn(payload.getResourceCrn());
        envDto.setName(payload.getResourceName());
        LOGGER.info("Flow entered into {}", envClustersDeleteState.name());
        return envDto;
    }

    private abstract static class AbstractEnvClustersDeleteAction<P extends ResourceCrnPayload>
            extends AbstractAction<EnvClustersDeleteState, EnvClustersDeleteStateSelectors, CommonContext, P> {

        protected AbstractEnvClustersDeleteAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters,
                StateContext<EnvClustersDeleteState, EnvClustersDeleteStateSelectors> stateContext,
                P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return payload;
        }

        @Override
        protected void prepareExecution(P payload, Map<Object, Object> variables) {
            if (payload != null) {
                MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
            } else {
                LOGGER.warn("Payload was null in prepareExecution so resourceCrn cannot be added to the MdcContext!");
            }
        }
    }

}
