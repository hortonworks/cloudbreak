package com.sequenceiq.environment.environment.flow.stop.config;

import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.ENV_STOP_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.ENV_STOP_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.STOP_DATAHUB_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.STOP_DATALAKE_STATE;
import static com.sequenceiq.environment.environment.flow.stop.EnvStopState.STOP_FREEIPA_STATE;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.ENV_STOP_DATAHUB_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.ENV_STOP_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.ENV_STOP_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FAILED_ENV_STOP_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FINALIZE_ENV_STOP_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.FINISH_ENV_STOP_EVENT;
import static com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors.HANDLED_FAILED_ENV_STOP_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.flow.stop.EnvStopState;
import com.sequenceiq.environment.environment.flow.stop.event.EnvStopStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class EnvStopFlowConfig extends AbstractFlowConfiguration<EnvStopState, EnvStopStateSelectors>
        implements RetryableFlowConfiguration<EnvStopStateSelectors> {

    private static final List<Transition<EnvStopState, EnvStopStateSelectors>> TRANSITIONS = new Transition.Builder<EnvStopState, EnvStopStateSelectors>()
            .defaultFailureEvent(FAILED_ENV_STOP_EVENT)

            .from(INIT_STATE).to(STOP_DATAHUB_STATE)
            .event(ENV_STOP_DATAHUB_EVENT).defaultFailureEvent()

            .from(STOP_DATAHUB_STATE).to(STOP_DATALAKE_STATE)
            .event(ENV_STOP_DATALAKE_EVENT).defaultFailureEvent()

            .from(STOP_DATALAKE_STATE).to(STOP_FREEIPA_STATE)
            .event(ENV_STOP_FREEIPA_EVENT).defaultFailureEvent()

            .from(STOP_FREEIPA_STATE).to(ENV_STOP_FINISHED_STATE)
            .event(FINISH_ENV_STOP_EVENT).defaultFailureEvent()

            .from(ENV_STOP_FINISHED_STATE).to(FINAL_STATE)
            .event(FINALIZE_ENV_STOP_EVENT).defaultFailureEvent()

            .build();

    protected EnvStopFlowConfig() {
        super(EnvStopState.class, EnvStopStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvStopState, EnvStopStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvStopState, EnvStopStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ENV_STOP_FAILED_STATE, HANDLED_FAILED_ENV_STOP_EVENT);
    }

    @Override
    public EnvStopStateSelectors[] getEvents() {
        return EnvStopStateSelectors.values();
    }

    @Override
    public EnvStopStateSelectors[] getInitEvents() {
        return new EnvStopStateSelectors[]{ENV_STOP_DATAHUB_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Stop environment";
    }

    @Override
    public EnvStopStateSelectors getFailHandledEvent() {
        return HANDLED_FAILED_ENV_STOP_EVENT;
    }
}
