package com.sequenceiq.environment.environment.flow.delete;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_NETWORK_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteHandlerSelectors.DELETE_RDBMS_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.FINALIZE_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.HANDLED_FAILED_ENV_DELETE_EVENT;

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
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors;
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
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
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
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
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
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                LOGGER.info("FREEIPA_DELETE_STARTED_STATE");
                sendEvent(context, DELETE_FREEIPA_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_DELETE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractVpcDeleteAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                environmentService
                        .findEnvironmentById(payload.getResourceId())
                        .ifPresent(env -> {
                            env.setName(generateArchiveName(env.getName()));
                            env.setDeletionTimestamp(new Date().getTime());
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
                            environment.setStatus(EnvironmentStatus.CORRUPTED);
                            environmentService.save(environment);
                        });
                LOGGER.info("ENV_DELETE_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_DELETE_EVENT.event(), payload);
            }
        };
    }

    private abstract class AbstractVpcDeleteAction<P extends Payload> extends AbstractAction<EnvDeleteState, EnvDeleteStateSelectors, CommonContext, P> {

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
    }

}
