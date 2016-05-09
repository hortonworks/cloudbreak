package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackStartFlowConfig extends AbstractFlowConfiguration<StackStartState, StackStartEvent> {

    private static final List<Transition<StackStartState, StackStartEvent>> TRANSITIONS = new Transition.Builder<StackStartState, StackStartEvent>()
            .defaultFailureEvent(START_FAILURE_EVENT)
            .from(INIT_STATE).to(START_STATE).event(START_EVENT).noFailureEvent()
            .from(START_STATE).to(START_FINISHED_STATE).event(START_FINISHED_EVENT).defaultFailureEvent()
            .from(START_FINISHED_STATE).to(FINAL_STATE).event(START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackStartState, StackStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, START_FAILED_STATE, START_FAIL_HANDLED_EVENT);

    public StackStartFlowConfig() {
        super(StackStartState.class, StackStartEvent.class);
    }

    @Override
    public StackStartEvent[] getEvents() {
        return StackStartEvent.values();
    }

    @Override
    protected List<Transition<StackStartState, StackStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackStartState, StackStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
