package com.sequenceiq.flow.core.chain.init.config;

import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_FAILED_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_FINISHED_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitEvent.FLOWCHAIN_INIT_TRIGGER_EVENT;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitState.FINAL_STATE;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitState.FLOWCHAIN_INIT_FAILED_STATE;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitState.FLOWCHAIN_INIT_FINISHED_STATE;
import static com.sequenceiq.flow.core.chain.init.config.FlowChainInitState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class FlowChainInitFlowConfig extends AbstractFlowConfiguration<FlowChainInitState, FlowChainInitEvent>
        implements RetryableFlowConfiguration<FlowChainInitEvent> {
    private static final List<Transition<FlowChainInitState, FlowChainInitEvent>> TRANSITIONS =
            new Transition.Builder<FlowChainInitState, FlowChainInitEvent>()
                    .defaultFailureEvent(FLOWCHAIN_INIT_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(FLOWCHAIN_INIT_FINISHED_STATE)
                    .event(FLOWCHAIN_INIT_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(FLOWCHAIN_INIT_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FLOWCHAIN_INIT_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FlowChainInitState, FlowChainInitEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FLOWCHAIN_INIT_FAILED_STATE, FLOWCHAIN_INIT_FAILHANDLED_EVENT);

    public FlowChainInitFlowConfig() {
        super(FlowChainInitState.class, FlowChainInitEvent.class);
    }

    @Override
    public FlowChainInitEvent[] getEvents() {
        return FlowChainInitEvent.values();
    }

    @Override
    public FlowChainInitEvent[] getInitEvents() {
        return new FlowChainInitEvent[]{
                FLOWCHAIN_INIT_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Flowchain initializer";
    }

    @Override
    public FlowChainInitEvent getRetryableEvent() {
        return FLOWCHAIN_INIT_FAILHANDLED_EVENT;
    }

    @Override
    protected List<Transition<FlowChainInitState, FlowChainInitEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<FlowChainInitState, FlowChainInitEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
