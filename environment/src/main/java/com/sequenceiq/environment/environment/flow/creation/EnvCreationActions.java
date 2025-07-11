package com.sequenceiq.environment.environment.flow.creation;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_WAITING_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_WAITING_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_CREATION_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_INITIALIZATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_INITIALIZATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_INITIALIZE_COMPUTE_CLUSTER_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_NETWORK_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_PUBLICKEY_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_PUBLICKEY_CREATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_VALIDATION_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ENVIRONMENT_VALIDATION_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_REQUIRED;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_COMPUTE_CLUSTER_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.VALIDATE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.WAIT_COMPUTE_CLUSTER_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINALIZE_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.HANDLED_FAILED_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.scheduled.sync.EnvironmentJobService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.events.sync.StructuredSynchronizerJobAdapter;
import com.sequenceiq.environment.events.sync.StructuredSynchronizerJobService;
import com.sequenceiq.environment.exception.ExternalizedComputeOperationFailedException;
import com.sequenceiq.environment.metrics.EnvironmentMetricService;
import com.sequenceiq.environment.metrics.MetricType;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvCreationActions.class);

    private final EnvironmentService environmentService;

    private final EventSenderService eventService;

    private final EnvironmentMetricService metricService;

    private final EnvironmentJobService environmentJobService;

    public EnvCreationActions(EnvironmentService environmentService,
            EventSenderService eventService,
            EnvironmentMetricService metricService,
            EnvironmentJobService environmentJobService) {
        this.environmentService = environmentService;
        this.eventService = eventService;
        this.metricService = metricService;
        this.environmentJobService = environmentJobService;
    }

    @Bean(name = "ENVIRONMENT_INITIALIZATION_STATE")
    public Action<?, ?> environmentInitAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Initialization of Environment has started. Current state is - ENVIRONMENT_INITIALIZATION_STATE");
                    environment.setStatus(EnvironmentStatus.ENVIRONMENT_INITIALIZATION_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_INITIALIZATION_STARTED);
                    sendEvent(context, INITIALIZE_ENVIRONMENT_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment init action went failed with EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_INITIALIZATION_FAILED, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to validate environment creation request! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "ENVIRONMENT_CREATION_VALIDATION_STATE")
    public Action<?, ?> environmentValidationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Validation of Environment has started. Current state is - ENVIRONMENT_CREATION_VALIDATION_STATE");
                    environment.setStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_VALIDATION_STARTED);
                    EnvironmentValidationDto environmentValidationDto = EnvironmentValidationDto.builder()
                            .withEnvironmentDto(environmentDto)
                            .withValidationType(ValidationType.ENVIRONMENT_CREATION)
                            .build();
                    sendEvent(context, VALIDATE_ENVIRONMENT_EVENT.selector(), environmentValidationDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment validation action went failed with EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_VALIDATION_FAILED, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to validate environment creation request! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "COMPUTE_CLUSTER_CREATION_STARTED_STATE")
    public Action<?, ?> computeClusterCreationStartedAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    if (environment.getDefaultComputeCluster().isCreate()) {
                        LOGGER.info("Creation of compute cluster has started. Current state is - COMPUTE_CLUSTER_CREATION_STARTED_STATE");
                        EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                        eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(),
                                ENVIRONMENT_INITIALIZE_COMPUTE_CLUSTER_STARTED);
                        sendEvent(context, CREATE_COMPUTE_CLUSTER_EVENT.selector(), environmentDto);
                    } else {
                        LOGGER.info("Creation of compute cluster is not required, proceed to the next state.");
                        sendEvent(context, START_NETWORK_CREATION_EVENT.selector(), payload);
                    }
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(payload.getResourceId(), payload.getResourceName(), null,
                            payload.getResourceCrn());
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_COMPUTE_CLUSTER_CREATION_FAILED,
                            context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create compute cluster for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "NETWORK_CREATION_STARTED_STATE")
    public Action<?, ?> networkCreationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Creation of Network has started. Current state is - NETWORK_CREATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_NETWORK_CREATION_STARTED);
                    sendEvent(context, CREATE_NETWORK_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment network creation action went failed with  EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_NETWORK_CREATION_FAILED, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create network for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "PUBLICKEY_CREATION_STARTED_STATE")
    public Action<?, ?> publickeyCreationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Creation of PublicKey has started. Current state is - PUBLICKEY_CREATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_PUBLICKEY_CREATION_STARTED);
                    sendEvent(context, CREATE_PUBLICKEY_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment public key creation action went failed with EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_PUBLICKEY_CREATION_FAILED, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create public key for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE")
    public Action<?, ?> resourceEncryptionInitializationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Initialization of resource encryption has started." +
                            " Current state is - ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(),
                            ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED);
                    sendEvent(context, INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment encryption init action went failed with  EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_FAILED,
                            context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create encryption resources for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "FREEIPA_CREATION_STARTED_STATE")
    public Action<?, ?> freeipaCreationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("Creation of FreeIPA has started. Current state is - FREEIPA_CREATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
                    environment.setStatusReason(null);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_FREEIPA_CREATION_STARTED);
                    sendEvent(context, CREATE_FREEIPA_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    LOGGER.debug("Environment freeipa creation action went failed with EnvCreationFailureEvent was: {}", failureEvent);
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_FREEIPA_CREATION_FAILED, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create freeipa for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "COMPUTE_CLUSTER_CREATION_WAITING_STATE")
    public Action<?, ?> computeClusterCreationWaitingAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    if (environment.getDefaultComputeCluster().isCreate()) {
                        LOGGER.info("Waiting for compute cluster creation. Current state is - COMPUTE_CLUSTER_CREATION_WAITING_STATE");
                        environment.setStatus(EnvironmentStatus.COMPUTE_CLUSTER_CREATION_IN_PROGRESS);
                        environment.setStatusReason(null);
                        environment = environmentService.save(environment);
                        EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                        eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(),
                                ENVIRONMENT_COMPUTE_CLUSTER_WAITING_STARTED);
                        sendEvent(context, WAIT_COMPUTE_CLUSTER_CREATION_EVENT.selector(), environmentDto);
                    } else {
                        LOGGER.info("Waiting for compute cluster creation is not required, proceed to the next state.");
                        sendEvent(context, FINISH_ENV_CREATION_EVENT.selector(), payload);
                    }
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(payload.getResourceId(), payload.getResourceName(), null,
                            payload.getResourceCrn());
                    eventService.sendEventAndNotificationForMissingEnv(payload, ENVIRONMENT_COMPUTE_CLUSTER_WAITING_FAILED,
                            context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to wait for compute cluster for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "ENV_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {

            @Inject
            private StructuredSynchronizerJobService structuredSynchronizerJobService;

            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Finished to create environment with payload {}", payload);
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            environment.setStatus(environment.getEnvironmentType().isHybrid() ? TRUST_SETUP_REQUIRED : AVAILABLE);
                            environment.setStatusReason(null);
                            Environment result = environmentService.save(environment);
                            structuredSynchronizerJobService.schedule(environment.getId(), StructuredSynchronizerJobAdapter.class, false);
                            environmentJobService.schedule(result.getId());
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(result);
                            metricService.incrementMetricCounter(MetricType.ENV_CREATION_FINISHED, environmentDto);
                            eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_CREATION_FINISHED);
                        }, () -> LOGGER.error("Cannot finish the creation of env, because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_CREATION_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationFailureEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationFailureEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.debug("Failed to create environment {} with payload {}", exception, payload);
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            ExternalizedComputeOperationFailedException externalizedException =
                                    ExceptionUtils.throwableOfType(exception, ExternalizedComputeOperationFailedException.class);
                            if (externalizedException != null) {
                                environment.setStatusReason(externalizedException.getMessage());
                                environment.setStatus(AVAILABLE);
                            } else {
                                environment.setStatusReason(exception.getMessage());
                                environment.setStatus(EnvironmentStatus.CREATE_FAILED);
                            }
                            environmentService.save(environment);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                            metricService.incrementMetricCounter(MetricType.ENV_CREATION_FAILED, environmentDto, exception);
                            eventService.sendEventAndNotification(environmentDto, context.getFlowTriggerUserCrn(), ENVIRONMENT_CREATION_FAILED,
                                    Set.of(exception.getMessage()));
                        }, () -> LOGGER.error("Cannot finish the creation of env, because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_CREATION_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

}
