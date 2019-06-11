package com.sequenceiq.environment.environment.flow.creation;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.CREATE_NETWORK_EVENT;
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

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class EnvCreationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvCreationActions.class);

    private final EnvironmentService environmentService;

    public EnvCreationActions(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Bean(name = "NETWORK_CREATION_STARTED_STATE")
    public Action<?, ?> networkCreationAction() {
        return new AbstractVpcCreateAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                var alma = context.getFlowTriggerUserCrn();
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                LOGGER.info("NETWORK_CREATION_STARTED_STATE");
                sendEvent(context, CREATE_NETWORK_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "FREEIPA_CREATION_STARTED_STATE")
    public Action<?, ?> freeipaCreationAction() {
        return new AbstractVpcCreateAction<>(EnvCreationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvCreationEvent payload, Map<Object, Object> variables) {
                EnvironmentDto envDto = new EnvironmentDto();
                envDto.setId(payload.getResourceId());
                LOGGER.info("FREEIPA_CREATION_STARTED_STATE");
                sendEvent(context, CREATE_FREEIPA_EVENT.selector(), envDto);
            }
        };
    }

    @Bean(name = "ENV_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractVpcCreateAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                environmentService
                        .findById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatus(EnvironmentStatus.AVAILABLE);
                            environmentService.save(environment);
                        });
                LOGGER.info("ENV_CREATION_FINISHED_STATE");
                sendEvent(context, FINALIZE_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "ENV_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractVpcCreateAction<>(Payload.class) {
            @Override
            protected void doExecute(CommonContext context, Payload payload, Map<Object, Object> variables) {
                environmentService
                        .findById(payload.getResourceId())
                        .ifPresent(environment -> {
                            environment.setStatus(EnvironmentStatus.CORRUPTED);
                            environmentService.save(environment);
                        });
                LOGGER.info("ENV_CREATION_FAILED_STATE");
                sendEvent(context, HANDLED_FAILED_ENV_CREATION_EVENT.event(), payload);
            }
        };
    }

    private abstract class AbstractVpcCreateAction<P extends Payload> extends AbstractAction<EnvCreationState, EnvCreationStateSelectors, CommonContext, P> {

        protected AbstractVpcCreateAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected CommonContext createFlowContext(FlowParameters flowParameters, StateContext<EnvCreationState, EnvCreationStateSelectors> stateContext,
                P payload) {
            return new CommonContext(flowParameters);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<CommonContext> flowContext, Exception ex) {
            return (Payload) () -> null;
        }
    }
}
