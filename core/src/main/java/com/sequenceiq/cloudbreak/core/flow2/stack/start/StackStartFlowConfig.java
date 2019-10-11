package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.COLLECT_METADATA_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.COLLECT_METADATA_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.STACK_START_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartEvent.START_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.COLLECTING_METADATA;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartState.START_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class StackStartFlowConfig extends AbstractFlowConfiguration<StackStartState, StackStartEvent>
        implements RetryableFlowConfiguration<StackStartEvent> {

    private static final List<Transition<StackStartState, StackStartEvent>> TRANSITIONS = new Builder<StackStartState, StackStartEvent>()
            .defaultFailureEvent(START_FAILURE_EVENT)
            .from(INIT_STATE).to(START_STATE).event(STACK_START_EVENT).noFailureEvent()
            .from(START_STATE).to(COLLECTING_METADATA).event(START_FINISHED_EVENT).defaultFailureEvent()
            .from(COLLECTING_METADATA).to(START_FINISHED_STATE).event(COLLECT_METADATA_FINISHED_EVENT).failureEvent(COLLECT_METADATA_FAILED_EVENT)
            .from(START_FINISHED_STATE).to(FINAL_STATE).event(START_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackStartState, StackStartEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, START_FAILED_STATE, START_FAIL_HANDLED_EVENT);

    private static final String FLOW_DISPLAY_NAME = "Start Stack";

    public StackStartFlowConfig() {
        super(StackStartState.class, StackStartEvent.class);
    }

    @Override
    public StackStartEvent[] getEvents() {
        return StackStartEvent.values();
    }

    @Override
    public StackStartEvent[] getInitEvents() {
        return new StackStartEvent[]{
                STACK_START_EVENT
        };
    }

    @Override
    protected List<Transition<StackStartState, StackStartEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected AbstractFlowConfiguration.FlowEdgeConfig<StackStartState, StackStartEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public String getDisplayName() {
        return FLOW_DISPLAY_NAME;
    }

    @Override
    public StackStartEvent getFailHandledEvent() {
        return START_FAIL_HANDLED_EVENT;
    }
}
