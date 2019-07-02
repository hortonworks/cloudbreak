package com.sequenceiq.environment.environment.flow.deletion;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_RDBMS_EVENT;
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

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvDeleteActions.class);

    private final EnvironmentService environmentService;

    public EnvDeleteActions(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Bean(name = "NETWORK_DELETE_STARTED_STATE")
    public Action<?, ?> networkDeleteAction() {
        return new AbstractVpcDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatus(EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS);
                            environmentService.save(environment);
                        });
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                envDto.setResourceCrn(payload.getResourceCrn());
                envDto.setResourceCrn(payload.getResourceName());
                LOGGER.info("NETWORK_DELETE_STARTED_STATE");
                sendEvent(context, DELETE_NETWORK_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "RDBMS_DELETE_STARTED_STATE")
    public Action<?, ?> rdbmsDeleteAction() {
        return new AbstractVpcDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatus(EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS);
                            environmentService.save(environment);
                        });
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                envDto.setResourceCrn(payload.getResourceCrn());
                envDto.setName(payload.getResourceName());
                LOGGER.info("RDBMS_DELETE_STARTED_STATE");
                sendEvent(context, DELETE_RDBMS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "FREEIPA_DELETE_STARTED_STATE")
    public Action<?, ?> freeipaDeleteAction() {
        return new AbstractVpcDeleteAction<>(EnvDeleteEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteEvent payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatus(EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS);
                            environmentService.save(environment);
                        });
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                envDto.setResourceCrn(payload.getResourceCrn());
                envDto.setResourceCrn(payload.getResourceName());
                LOGGER.info("FREEIPA_DELETE_STARTED_STATE");
                sendEvent(context, DELETE_FREEIPA_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_DELETE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractVpcDeleteAction<>(ResourceCrnPayload.class) {
            @Override
            protected void doExecute(CommonContext context, ResourceCrnPayload payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(env -> {
                            env.setName(generateArchiveName(env.getName()));
                            env.setDeletionTimestamp(new Date().getTime());
                            env.setStatusReason(null);
                            env.setStatus(EnvironmentStatus.ARCHIVED);
                            env.setArchived(true);
                            environmentService.save(env);
                        });
                LOGGER.info("ENV_DELETE_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_DELETE_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_DELETE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractVpcDeleteAction<>(EnvDeleteFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvDeleteFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.warn("Failed to delete environment", payload.getException());
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatusReason(payload.getException().getMessage());
                            environment.setStatus(EnvironmentStatus.DELETE_FAILED);
                            environmentService.save(environment);
                        });
                LOGGER.info("ENV_DELETE_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_DELETE_EVENT.event(), payload);
            }
        };
    }

    private abstract class AbstractVpcDeleteAction<P extends ResourceCrnPayload>
            extends AbstractAction<EnvDeleteState, EnvDeleteStateSelectors, CommonContext, P> {

        protected AbstractVpcDeleteAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvDeleteState, EnvDeleteStateSelectors> stateContext,
                P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return (Payload) () -> null;
        }

        @Override
        protected void prepareExecution(P payload, Map<Object, Object> variables) {
            MdcContext.builder().resourceCrn(payload.getResourceCrn()).buildMdc();
        }
    }

}
