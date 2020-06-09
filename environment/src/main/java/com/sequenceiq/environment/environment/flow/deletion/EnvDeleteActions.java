package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_CLUSTER_DEFINITION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_IDBROKER_MAPPINGS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PREREQUISITES_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_RDBMS_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_S3GUARD_TABLE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_UMS_RESOURCE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINALIZE_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.HANDLED_FAILED_ENV_DELETE_EVENT;

import java.util.Date;
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
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors;
import com.sequenceiq.environment.environment.flow.start.EnvStartState;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.notification.NotificationService;

@Configuration
public class EnvDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvDeleteActions.class);

    private final EnvironmentService environmentService;

    private final NotificationService notificationService;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentMetricService metricService;

    public EnvDeleteActions(EnvironmentService environmentService, NotificationService notificationService,
            EnvironmentResponseConverter environmentResponseConverter, EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentMetricService metricService) {
        this.environmentService = environmentService;
        this.notificationService = notificationService;
        this.environmentResponseConverter = environmentResponseConverter;
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.metricService = metricService;
    }

    @Bean(name = "FREEIPA_DELETE_STARTED_STATE")
    public Action<?, ?> freeipaDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_FREEIPA_DELETION_STARTED, EnvDeleteState.FREEIPA_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_FREEIPA_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "RDBMS_DELETE_STARTED_STATE")
    public Action<?, ?> rdbmsDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_DATABASE_DELETION_STARTED, EnvDeleteState.RDBMS_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_RDBMS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "PUBLICKEY_DELETE_STARTED_STATE")
    public Action<?, ?> publickeyDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_PUBLICKEY_DELETION_STARTED, EnvDeleteState.PUBLICKEY_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_PUBLICKEY_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "NETWORK_DELETE_STARTED_STATE")
    public Action<?, ?> networkDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_NETWORK_DELETION_STARTED, EnvDeleteState.NETWORK_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_NETWORK_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "IDBROKER_MAPPINGS_DELETE_STARTED_STATE")
    public Action<?, ?> idbmmsDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_IDBROKER_MAPPINGS_DELETION_STARTED, EnvDeleteState.IDBROKER_MAPPINGS_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_IDBROKER_MAPPINGS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "S3GUARD_TABLE_DELETE_STARTED_STATE")
    public Action<?, ?> s3GuardTableDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_S3GUARD_TABLE_DELETION_STARTED, EnvDeleteState.S3GUARD_TABLE_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_S3GUARD_TABLE_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "CLUSTER_DEFINITION_DELETE_STARTED_STATE")
    public Action<?, ?> clusterDefinitionDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.CLUSTER_DEFINITION_DELETE_PROGRESS,
                                ResourceEvent.ENVIRONMENT_CLUSTER_DEFINITION_DELETE_STARTED, EnvDeleteState.CLUSTER_DEFINITION_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_CLUSTER_DEFINITION_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "UMS_RESOURCE_DELETE_STARTED_STATE")
    public Action<?, ?> umsResourceDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.PREREQUISITES_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_UMS_RESOURCE_DELETION_STARTED, EnvDeleteState.UMS_RESOURCE_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_UMS_RESOURCE_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "PREREQUISITES_DELETE_STARTED_STATE")
    public Action<?, ?> prerequisitesDeleteAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.PREREQUISITES_DELETE_IN_PROGRESS,
                                ResourceEvent.ENVIRONMENT_PREREQUISITES_DELETION_STARTED, EnvDeleteState.PREREQUISITES_DELETE_STARTED_STATE);
                sendEvent(context, DELETE_PREREQUISITES_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_DELETE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvDeleteAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(env -> {
                            String originalName = env.getName();
                            env.setName(generateArchiveName(env.getName()));
                            env.setDeletionTimestamp(new Date().getTime());
                            env.setStatusReason(null);
                            env.setStatus(EnvironmentStatus.ARCHIVED);
                            env.setArchived(true);
                            env.setProxyConfig(null);
                            Environment result = environmentService.save(env);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(result);
                            SimpleEnvironmentResponse simpleResponse = environmentResponseConverter.dtoToSimpleResponse(environmentDto);
                            simpleResponse.setName(originalName);
                            metricService.incrementMetricCounter(MetricType.ENV_DELETION_FINISHED, environmentDto);
                            notificationService.send(ResourceEvent.ENVIRONMENT_DELETION_FINISHED, simpleResponse, context.getFlowTriggerUserCrn());
                        }, () -> LOGGER.error("Cannot finish the delete flow because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_DELETE_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_DELETE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_DELETE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvDeleteAction<>(EnvDeleteFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to delete environment", payload.getException());
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            metricService.incrementMetricCounter(MetricType.ENV_DELETION_FAILED, environment, payload.getException());
                            environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.DELETE_FAILED,
                                    ResourceEvent.ENVIRONMENT_DELETION_FAILED, EnvStartState.ENV_START_FAILED_STATE);
                        }, () -> LOGGER.error("Cannot set delete failed to env because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_DELETE_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_DELETE_EVENT.event(), payload);
            }
        };
    }

    private abstract static class AbstractEnvDeleteAction<P extends ResourceCrnPayload>
            extends AbstractAction<EnvDeleteState, EnvDeleteStateSelectors, CommonContext, P> {

        protected AbstractEnvDeleteAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvDeleteState, EnvDeleteStateSelectors> stateContext,
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
