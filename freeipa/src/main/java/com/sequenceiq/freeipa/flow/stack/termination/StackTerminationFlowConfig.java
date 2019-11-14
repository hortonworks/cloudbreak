package com.sequenceiq.freeipa.flow.stack.termination;

import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.CLUSTER_PROXY_DEREGISTRATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.REMOVE_MACHINE_USER_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FINALIZED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationEvent.TERMINATION_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.DEREGISTER_CLUSTERPROXY_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.REMOVE_MACHINE_USER_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.stack.termination.StackTerminationState.TERMINATION_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class StackTerminationFlowConfig extends AbstractFlowConfiguration<StackTerminationState, StackTerminationEvent> {

    private static final List<Transition<StackTerminationState, StackTerminationEvent>> TRANSITIONS =
            new Builder<StackTerminationState, StackTerminationEvent>()
                    .defaultFailureEvent(TERMINATION_FAILED_EVENT)
                    .from(INIT_STATE).to(DEREGISTER_CLUSTERPROXY_STATE).event(TERMINATION_EVENT).noFailureEvent()
                    .from(DEREGISTER_CLUSTERPROXY_STATE).to(REMOVE_MACHINE_USER_STATE).event(CLUSTER_PROXY_DEREGISTRATION_FINISHED_EVENT).noFailureEvent()
                    .from(REMOVE_MACHINE_USER_STATE).to(TERMINATION_STATE).event(REMOVE_MACHINE_USER_FINISHED_EVENT).noFailureEvent()
                    .from(TERMINATION_STATE).to(TERMINATION_FINISHED_STATE).event(TERMINATION_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<StackTerminationState, StackTerminationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_FAILED_STATE, STACK_TERMINATION_FAIL_HANDLED_EVENT);

    public StackTerminationFlowConfig() {
        super(StackTerminationState.class, StackTerminationEvent.class);
    }

    @Override
    protected List<Transition<StackTerminationState, StackTerminationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackTerminationState, StackTerminationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public StackTerminationEvent[] getEvents() {
        return StackTerminationEvent.values();
    }

    @Override
    public StackTerminationEvent[] getInitEvents() {
        return new StackTerminationEvent[]{TERMINATION_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "Terminate stack";
    }
}
