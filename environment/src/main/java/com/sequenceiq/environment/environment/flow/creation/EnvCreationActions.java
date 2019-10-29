package com.sequenceiq.environment.environment.flow.creation;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.VALIDATE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINALIZE_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.HANDLED_FAILED_ENV_CREATION_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.EnvironmentApiConverter;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.notification.NotificationService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

@Configuration
public class EnvCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvCreationActions.class);

    private final EnvironmentService environmentService;

    private final NotificationService notificationService;

    private final EnvironmentApiConverter environmentApiConverter;

    public EnvCreationActions(EnvironmentService environmentService, NotificationService notificationService,
            EnvironmentApiConverter environmentApiConverter) {
        this.environmentService = environmentService;
        this.notificationService = notificationService;
        this.environmentApiConverter = environmentApiConverter;
    }

    @Bean(name = "ENVIRONMENT_CREATION_VALIDATION_STATE")
    public Action<?, ?> environmentValidationAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService.findEnvironmentById(payload.getResourceId()).ifPresentOrElse(environment -> {
                    LOGGER.info("ENVIRONMENT_CREATION_VALIDATION_STATE");
                    environment.setStatus(EnvironmentStatus.ENVIRONMENT_VALIDATION_IN_PROGRESS);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(ResourceEvent.ENVIRONMENT_VALIDATION_STARTED, simpleResponse, context.getFlowTriggerUserCrn());
                    sendEvent(context, VALIDATE_ENVIRONMENT_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    notificationService.send(ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, payload, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to validate environment creation request! No environment found with id '{}'.", payload.getResourceId());
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
                    LOGGER.info("NETWORK_CREATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(ResourceEvent.ENVIRONMENT_NETWORK_CREATION_STARTED, simpleResponse, context.getFlowTriggerUserCrn());
                    sendEvent(context, CREATE_NETWORK_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    notificationService.send(ResourceEvent.ENVIRONMENT_NETWORK_CREATION_FAILED, payload, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create network for environment! No environment found with id '{}'.", payload.getResourceId());
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
                    LOGGER.info("FREEIPA_CREATION_STARTED_STATE");
                    environment.setStatus(EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS);
                    environment = environmentService.save(environment);
                    EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                    SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                    notificationService.send(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_STARTED, simpleResponse, context.getFlowTriggerUserCrn());
                    sendEvent(context, CREATE_FREEIPA_EVENT.selector(), environmentDto);
                }, () -> {
                    EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                            payload.getResourceId(),
                            payload.getResourceName(),
                            null,
                            payload.getResourceCrn());
                    notificationService.send(ResourceEvent.ENVIRONMENT_FREEIPA_CREATION_FAILED, payload, context.getFlowTriggerUserCrn());
                    LOGGER.warn("Failed to create freeipa for environment! No environment found with id '{}'.", payload.getResourceId());
                    sendEvent(context, failureEvent);
                });
            }
        };
    }

    @Bean(name = "ENV_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractEnvironmentCreationAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            environment.setStatusReason(null);
                            environment.setStatus(EnvironmentStatus.AVAILABLE);
                            Environment result = environmentService.save(environment);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(result);
                            SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                            notificationService.send(ResourceEvent.ENVIRONMENT_CREATION_FINISHED, simpleResponse, context.getFlowTriggerUserCrn());
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
                LOGGER.warn("Failed to create environment", payload.getException());
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresentOrElse(environment -> {
                            environment.setStatusReason(payload.getException().getMessage());
                            environment.setStatus(EnvironmentStatus.CREATE_FAILED);
                            environmentService.save(environment);
                            EnvironmentDto environmentDto = environmentService.getEnvironmentDto(environment);
                            SimpleEnvironmentResponse simpleResponse = environmentApiConverter.dtoToSimpleResponse(environmentDto);
                            notificationService.send(ResourceEvent.ENVIRONMENT_CREATION_FAILED, simpleResponse, context.getFlowTriggerUserCrn());
                        }, () -> LOGGER.error("Cannot finish the creation of env, because the environment does not exist: {}. "
                                + "But the flow will continue, how can this happen?", payload.getResourceId()));
                LOGGER.info("Flow entered into ENV_CREATION_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    private abstract class AbstractEnvironmentCreationAction<P extends ResourceCrnPayload>
            extends AbstractAction<EnvCreationState, EnvCreationStateSelectors, CommonContext, P> {

        protected AbstractEnvironmentCreationAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvCreationState, EnvCreationStateSelectors> stateContext,
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
