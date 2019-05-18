package com.sequenceiq.environment.env.flow.creation;

import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationHandlerSelectors.CREATE_RDBMS_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors.FINALIZE_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors.HANDLED_FAILED_ENV_CREATION_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.environment.env.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.env.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.environment.env.service.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.NetworkCreationService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvCreationActions.class);

    private final EnvironmentService environmentService;

    private final NetworkCreationService networkCreationService;

    public EnvCreationActions(EnvironmentService environmentService, NetworkCreationService networkCreationService) {
        this.environmentService = environmentService;
        this.networkCreationService = networkCreationService;
    }

    @Bean(name = "NETWORK_CREATION_STARTED_STATE")
    public Action<?, ?> networkCreationAction() {
        return new AbstractVpcCreateAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentService.getById(payload.getResourceId());
                LOGGER.info("NETWORK_CREATION_STARTED_STATE");
                sendEvent(context.getFlowId(), CREATE_NETWORK_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "RDBMS_CREATION_STARTED_STATE")
    public Action<?, ?> rdbmsCreationAction() {
        return new AbstractVpcCreateAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentService.getById(payload.getResourceId());
                LOGGER.info("RDBMS_CREATION_STARTED_STATE");
                sendEvent(context.getFlowId(), CREATE_RDBMS_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "FREEIPA_CREATION_STARTED_STATE")
    public Action<?, ?> freeipaCreationAction() {
        return new AbstractVpcCreateAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = environmentService.getById(payload.getResourceId());
                LOGGER.info("FREEIPA_CREATION_STARTED_STATE");
                sendEvent(context.getFlowId(), CREATE_FREEIPA_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractVpcCreateAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                LOGGER.info("ENV_CREATION_FINISHED_STATE");
                sendEvent(context.getFlowId(), FINALIZE_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractVpcCreateAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                LOGGER.info("ENV_CREATION_FAILED_STATE");
                sendEvent(context.getFlowId(), HANDLED_FAILED_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    private abstract class AbstractVpcCreateAction<P extends Payload> extends AbstractAction<EnvCreationState, EnvCreationStateSelectors, CommonContext, P> {

        protected AbstractVpcCreateAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(String flowId, StateContext<EnvCreationState, EnvCreationStateSelectors> stateContext, P payload) {
            return new CommonContext(flowId);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return (Payload) () -> null;
        }
    }
}
