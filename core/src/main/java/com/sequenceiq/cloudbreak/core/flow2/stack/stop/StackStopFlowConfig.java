package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopEvent.STOP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.stop.StackStopState.STOP_STATE;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackStopFlowConfig extends AbstractFlowConfiguration<StackStopState, StackStopEvent> {

    private static final List<Transition<StackStopState, StackStopEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, STOP_STATE, STOP_EVENT),
            new Transition<>(STOP_STATE, STOP_FINISHED_STATE, STOP_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackStopState, StackStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOP_FINISHED_STATE, STOP_FINALIZED_EVENT, STOP_FAILED_STATE, STOP_FAIL_HANDLED_EVENT);

    public StackStopFlowConfig() {
        super(StackStopEvent.class);
    }

    @Override
    public List<StackStopEvent> getFlowTriggerEvents() {
        return Collections.singletonList(STOP_EVENT);
    }

    @Override
    public StackStopEvent[] getEvents() {
        return StackStopEvent.values();
    }

    @Override
    protected List<Transition<StackStopState, StackStopEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackStopState, StackStopEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
