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

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackStopFlowConfig extends AbstractFlowConfiguration<StackStopState, StackStopEvent> {

    private static final List<Transition<StackStopState, StackStopEvent>> TRANSITIONS = new Transition.Builder<StackStopState, StackStopEvent>()
            .defaultFailureEvent(StackStopEvent.STOP_FAILURE_EVENT)
            .from(INIT_STATE).to(STOP_STATE).event(STOP_EVENT).noFailureEvent()
            .from(STOP_STATE).to(STOP_FINISHED_STATE).event(STOP_FINISHED_EVENT).defaultFailureEvent()
            .from(STOP_FINISHED_STATE).to(FINAL_STATE).event(STOP_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackStopState, StackStopEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, STOP_FAILED_STATE, STOP_FAIL_HANDLED_EVENT);

    public StackStopFlowConfig() {
        super(StackStopState.class, StackStopEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(StackStopFlowTriggerCondition.class);
    }

    @Override
    public StackStopEvent[] getEvents() {
        return StackStopEvent.values();
    }

    @Override
    public StackStopEvent[] getInitEvents() {
        return new StackStopEvent[]{
                StackStopEvent.STOP_EVENT
        };
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
