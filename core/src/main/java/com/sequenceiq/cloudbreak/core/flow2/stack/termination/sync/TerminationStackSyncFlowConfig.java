package com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync;

import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncEvent.TERMINATION_STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncEvent.TERMINATION_SYNC_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncEvent.TERMINATION_SYNC_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncEvent.TERMINATION_SYNC_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncState.TERMINATION_SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncState.TERMINATION_SYNC_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync.TerminationStackSyncState.TERMINATION_SYNC_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class TerminationStackSyncFlowConfig extends StackStatusFinalizerAbstractFlowConfig<TerminationStackSyncState, TerminationStackSyncEvent> {
    private static final List<Transition<TerminationStackSyncState, TerminationStackSyncEvent>> TRANSITIONS =
            new Builder<TerminationStackSyncState, TerminationStackSyncEvent>()
                    .defaultFailureEvent(TerminationStackSyncEvent.TERMINATION_SYNC_FAILURE_EVENT)
                    .from(INIT_STATE).to(TERMINATION_SYNC_STATE).event(TERMINATION_STACK_SYNC_EVENT).noFailureEvent()
                    .from(TERMINATION_SYNC_STATE).to(TERMINATION_SYNC_FINISHED_STATE).event(TERMINATION_SYNC_FINISHED_EVENT).defaultFailureEvent()
                    .from(TERMINATION_SYNC_FINISHED_STATE).to(FINAL_STATE).event(TERMINATION_SYNC_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<TerminationStackSyncState, TerminationStackSyncEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, TERMINATION_SYNC_FAILED_STATE, TERMINATION_SYNC_FAIL_HANDLED_EVENT);

    public TerminationStackSyncFlowConfig() {
        super(TerminationStackSyncState.class, TerminationStackSyncEvent.class);
    }

    @Override
    public TerminationStackSyncEvent[] getEvents() {
        return TerminationStackSyncEvent.values();
    }

    @Override
    public TerminationStackSyncEvent[] getInitEvents() {
        return new TerminationStackSyncEvent[]{
                TERMINATION_STACK_SYNC_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Termination sync stack";
    }

    @Override
    protected List<Transition<TerminationStackSyncState, TerminationStackSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<TerminationStackSyncState, TerminationStackSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
