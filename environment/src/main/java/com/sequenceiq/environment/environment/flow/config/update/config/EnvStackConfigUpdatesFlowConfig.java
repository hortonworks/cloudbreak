package com.sequenceiq.environment.environment.flow.config.update.config;

import static com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState.FINAL_STATE;
import static com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState.INIT_STATE;
import static com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FAILED_STATE;
import static com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_FINISHED_STATE;
import static com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState.STACK_CONFIG_UPDATES_START_STATE;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.ENV_STACK_CONFIG_UPDATES_START_EVENT;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.FINALIZE_ENV_STACK_CONIFG_UPDATES_EVENT;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.FINISH_ENV_STACK_CONFIG_UPDATES_EVENT;
import static com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors.HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;

import com.sequenceiq.environment.environment.flow.config.update.EnvStackConfigUpdatesState;
import com.sequenceiq.environment.environment.flow.config.update.event.EnvStackConfigUpdatesStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EnvStackConfigUpdatesFlowConfig extends
    AbstractFlowConfiguration<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors>
    implements RetryableFlowConfiguration<EnvStackConfigUpdatesStateSelectors> {

    private static final List<Transition<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors>>
        TRANSITIONS = new Transition.Builder<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors>()
        .defaultFailureEvent(FAILED_ENV_STACK_CONIFG_UPDATES_EVENT)

        .from(INIT_STATE).to(STACK_CONFIG_UPDATES_START_STATE)
        .event(ENV_STACK_CONFIG_UPDATES_START_EVENT).defaultFailureEvent()

        .from(STACK_CONFIG_UPDATES_START_STATE).to(STACK_CONFIG_UPDATES_FINISHED_STATE)
        .event(FINISH_ENV_STACK_CONFIG_UPDATES_EVENT).defaultFailureEvent()

        .from(STACK_CONFIG_UPDATES_FINISHED_STATE).to(FINAL_STATE)
        .event(FINALIZE_ENV_STACK_CONIFG_UPDATES_EVENT).defaultFailureEvent()

        .build();

    protected EnvStackConfigUpdatesFlowConfig() {
        super(EnvStackConfigUpdatesState.class, EnvStackConfigUpdatesStateSelectors.class);
    }

    @Override
    protected List<Transition<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<EnvStackConfigUpdatesState, EnvStackConfigUpdatesStateSelectors> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STACK_CONFIG_UPDATES_FAILED_STATE,
            HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT);
    }

    @Override
    public EnvStackConfigUpdatesStateSelectors[] getEvents() {
        return EnvStackConfigUpdatesStateSelectors.values();
    }

    @Override
    public EnvStackConfigUpdatesStateSelectors[] getInitEvents() {
        return new EnvStackConfigUpdatesStateSelectors[]{ENV_STACK_CONFIG_UPDATES_START_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Update all Environment Clusters configuration";
    }

    @Override
    public EnvStackConfigUpdatesStateSelectors getRetryableEvent() {
        return HANDLE_FAILED_ENV_STACK_CONIFG_UPDATES_EVENT;
    }
}
