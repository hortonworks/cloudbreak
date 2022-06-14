package com.sequenceiq.environment.environment.flow.creation.config;

import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.ENVIRONMENT_CREATION_VALIDATION_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.ENVIRONMENT_INITIALIZATION_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.ENV_CREATION_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.ENV_CREATION_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.FREEIPA_CREATION_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.NETWORK_CREATION_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.PUBLICKEY_CREATION_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.EnvCreationState.STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINALIZE_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FINISH_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.HANDLED_FAILED_ENV_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_ENVIRONMENT_VALIDATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_PUBLICKEY_CREATION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.creation.EnvCreationState;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvCreationFlowConfig extends AbstractFlowConfiguration<EnvCreationState, EnvCreationStateSelectors>
        implements RetryableFlowConfiguration<EnvCreationStateSelectors> {

    private static final List<Transition<EnvCreationState, EnvCreationStateSelectors>> TRANSITIONS
            = new Transition.Builder<EnvCreationState, EnvCreationStateSelectors>().defaultFailureEvent(FAILED_ENV_CREATION_EVENT)

            .from(INIT_STATE).to(ENVIRONMENT_INITIALIZATION_STATE)
            .event(START_ENVIRONMENT_INITIALIZATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(ENVIRONMENT_INITIALIZATION_STATE).to(ENVIRONMENT_CREATION_VALIDATION_STATE)
            .event(START_ENVIRONMENT_VALIDATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(ENVIRONMENT_CREATION_VALIDATION_STATE).to(STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_STARTED_STATE)
            .event(START_STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_STARTED_STATE).to(NETWORK_CREATION_STARTED_STATE)
            .event(START_NETWORK_CREATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(NETWORK_CREATION_STARTED_STATE).to(PUBLICKEY_CREATION_STARTED_STATE)
            .event(START_PUBLICKEY_CREATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(PUBLICKEY_CREATION_STARTED_STATE).to(ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE)
            .event(START_ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE).to(FREEIPA_CREATION_STARTED_STATE)
            .event(START_FREEIPA_CREATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(FREEIPA_CREATION_STARTED_STATE).to(ENV_CREATION_FINISHED_STATE)
            .event(FINISH_ENV_CREATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .from(ENV_CREATION_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENV_CREATION_EVENT)
            .failureState(ENV_CREATION_FAILED_STATE)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<EnvCreationState, EnvCreationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENV_CREATION_FAILED_STATE, HANDLED_FAILED_ENV_CREATION_EVENT);

    public EnvCreationFlowConfig() {
        super(EnvCreationState.class, EnvCreationStateSelectors.class);
    }

    @Override
    public EnvCreationStateSelectors[] getEvents() {
        return EnvCreationStateSelectors.values();
    }

    @Override
    public EnvCreationStateSelectors[] getInitEvents() {
        return new EnvCreationStateSelectors[]{
                START_ENVIRONMENT_INITIALIZATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Create environment";
    }

    @Override
    public EnvCreationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ENV_CREATION_EVENT;
    }

    @Override
    protected List<Transition<EnvCreationState, EnvCreationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvCreationState, EnvCreationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public OperationType getFlowOperationType() {
        return OperationType.PROVISION;
    }
}
