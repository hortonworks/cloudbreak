package com.sequenceiq.environment.environment.flow.delete.config;

import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.ENV_DELETE_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.ENV_DELETE_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.FREEIPA_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.NETWORK_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.delete.EnvDeleteState.RDBMS_DELETE_STARTED_STATE;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.FINALIZE_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.HANDLED_FAILED_ENV_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.START_FREEIPA_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;
import static com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors.START_RDBMS_DELETE_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.delete.EnvDeleteState;
import com.sequenceiq.environment.environment.flow.delete.event.EnvDeleteStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvDeleteFlowConfig extends AbstractFlowConfiguration<EnvDeleteState, EnvDeleteStateSelectors>
        implements RetryableFlowConfiguration<EnvDeleteStateSelectors> {

    private static final List<Transition<EnvDeleteState, EnvDeleteStateSelectors>> TRANSITIONS
            = new Transition.Builder<EnvDeleteState, EnvDeleteStateSelectors>().defaultFailureEvent(EnvDeleteStateSelectors.FAILED_ENV_DELETE_EVENT)

            .from(INIT_STATE).to(NETWORK_DELETE_STARTED_STATE)
            .event(START_NETWORK_DELETE_EVENT).defaultFailureEvent()

            .from(NETWORK_DELETE_STARTED_STATE).to(RDBMS_DELETE_STARTED_STATE)
            .event(START_RDBMS_DELETE_EVENT).defaultFailureEvent()

            .from(RDBMS_DELETE_STARTED_STATE).to(FREEIPA_DELETE_STARTED_STATE)
            .event(START_FREEIPA_DELETE_EVENT).defaultFailureEvent()

            .from(FREEIPA_DELETE_STARTED_STATE).to(ENV_DELETE_FINISHED_STATE)
            .event(FINISH_ENV_DELETE_EVENT).defaultFailureEvent()

            .from(ENV_DELETE_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENV_DELETE_EVENT).defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<EnvDeleteState, EnvDeleteStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENV_DELETE_FAILED_STATE, HANDLED_FAILED_ENV_DELETE_EVENT);

    public EnvDeleteFlowConfig() {
        super(EnvDeleteState.class, EnvDeleteStateSelectors.class);
    }

    @Override
    public EnvDeleteStateSelectors[] getEvents() {
        return EnvDeleteStateSelectors.values();
    }

    @Override
    public EnvDeleteStateSelectors[] getInitEvents() {
        return new EnvDeleteStateSelectors[]{
                START_NETWORK_DELETE_EVENT
        };
    }

    @Override
    public EnvDeleteStateSelectors getFailHandledEvent() {
        return HANDLED_FAILED_ENV_DELETE_EVENT;
    }

    @Override
    protected List<Transition<EnvDeleteState, EnvDeleteStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvDeleteState, EnvDeleteStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
