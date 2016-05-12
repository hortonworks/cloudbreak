package com.sequenceiq.cloudbreak.core.flow2.stack.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.DOWNSCALE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackDownscaleConfig extends AbstractFlowConfiguration<StackDownscaleState, StackDownscaleEvent> {
    private static final List<Transition<StackDownscaleState, StackDownscaleEvent>> TRANSITIONS =
            new Transition.Builder<StackDownscaleState, StackDownscaleEvent>()
                .defaultFailureEvent(DOWNSCALE_FAILURE_EVENT)
                .from(INIT_STATE).to(DOWNSCALE_STATE).event(DOWNSCALE_EVENT).noFailureEvent()
                .from(DOWNSCALE_STATE).to(DOWNSCALE_FINISHED_STATE).event(DOWNSCALE_FINISHED_EVENT).defaultFailureEvent()
                .from(DOWNSCALE_FINISHED_STATE).to(FINAL_STATE).event(DOWNSCALE_FINALIZED_EVENT).defaultFailureEvent()
                .build();

    private static final FlowEdgeConfig<StackDownscaleState, StackDownscaleEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DOWNSCALE_FAILED_STATE, DOWNSCALE_FAIL_HANDLED_EVENT);

    public StackDownscaleConfig() {
        super(StackDownscaleState.class, StackDownscaleEvent.class);
    }

    @Override
    public StackDownscaleEvent[] getEvents() {
        return StackDownscaleEvent.values();
    }

    @Override
    protected List<Transition<StackDownscaleState, StackDownscaleEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackDownscaleState, StackDownscaleEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
