package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination.InstanceTerminationState.TERMINATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.FlowTriggerCondition;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class InstanceTerminationFlowConfig extends AbstractFlowConfiguration<InstanceTerminationState, InstanceTerminationEvent> {

    private static final List<Transition<InstanceTerminationState, InstanceTerminationEvent>> TRANSITIONS =
            new Transition.Builder<InstanceTerminationState, InstanceTerminationEvent>()
                    .defaultFailureEvent(TERMINATION_FAILED_EVENT)
                    .from(INIT_STATE).to(TERMINATION_STATE).event(TERMINATION_EVENT).noFailureEvent()
                    .from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<InstanceTerminationState, InstanceTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, TERMINATION_FAIL_HANDLED_EVENT);

    public InstanceTerminationFlowConfig() {
        super(InstanceTerminationState.class, InstanceTerminationEvent.class);
    }

    @Override
    public FlowTriggerCondition getFlowTriggerCondition() {
        return getApplicationContext().getBean(InstanceTerminationFlowTriggerCondition.class);
    }

    @Override
    protected List<Transition<InstanceTerminationState, InstanceTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<InstanceTerminationState, InstanceTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public InstanceTerminationEvent[] getEvents() {
        return InstanceTerminationEvent.values();
    }

    @Override
    public InstanceTerminationEvent[] getInitEvents() {
        return new InstanceTerminationEvent[] {
            InstanceTerminationEvent.TERMINATION_EVENT
        };
    }
}
