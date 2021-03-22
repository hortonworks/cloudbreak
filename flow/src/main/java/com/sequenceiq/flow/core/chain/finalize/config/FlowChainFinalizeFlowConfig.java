package com.sequenceiq.flow.core.chain.finalize.config;

import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_FAILED_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_FINISHED_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeEvent.FLOWCHAIN_FINALIZE_TRIGGER_EVENT;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState.FINAL_STATE;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FAILED_STATE;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE;
import static com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class FlowChainFinalizeFlowConfig extends AbstractFlowConfiguration<FlowChainFinalizeState, FlowChainFinalizeEvent>
        implements RetryableFlowConfiguration<FlowChainFinalizeEvent> {
    private static final List<Transition<FlowChainFinalizeState, FlowChainFinalizeEvent>> TRANSITIONS =
            new Transition.Builder<FlowChainFinalizeState, FlowChainFinalizeEvent>()
                    .defaultFailureEvent(FLOWCHAIN_FINALIZE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(FLOWCHAIN_FINALIZE_FINISHED_STATE)
                    .event(FLOWCHAIN_FINALIZE_TRIGGER_EVENT)
                    .noFailureEvent()

                    .from(FLOWCHAIN_FINALIZE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FLOWCHAIN_FINALIZE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<FlowChainFinalizeState, FlowChainFinalizeEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FLOWCHAIN_FINALIZE_FAILED_STATE, FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT);

    public FlowChainFinalizeFlowConfig() {
        super(FlowChainFinalizeState.class, FlowChainFinalizeEvent.class);
    }

    @Override
    public FlowChainFinalizeEvent[] getEvents() {
        return FlowChainFinalizeEvent.values();
    }

    @Override
    public FlowChainFinalizeEvent[] getInitEvents() {
        return new FlowChainFinalizeEvent[]{
                FLOWCHAIN_FINALIZE_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Flowchain finalizer";
    }

    @Override
    public FlowChainFinalizeEvent getRetryableEvent() {
        return FLOWCHAIN_FINALIZE_FAILHANDLED_EVENT;
    }

    @Override
    protected List<Transition<FlowChainFinalizeState, FlowChainFinalizeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<FlowChainFinalizeState, FlowChainFinalizeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
