package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_STATE;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;

@Component
public class StackSyncFlowConfig extends AbstractFlowConfiguration<StackSyncState, StackSyncEvent> {

    private static final List<Transition<StackSyncState, StackSyncEvent>> TRANSITIONS = Arrays.asList(
            new Transition<>(INIT_STATE, SYNC_STATE, SYNC_EVENT),
            new Transition<>(SYNC_STATE, SYNC_FINISHED_STATE, SYNC_FINISHED_EVENT)
    );
    private static final FlowEdgeConfig<StackSyncState, StackSyncEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SYNC_FINISHED_STATE, SYNC_FINALIZED_EVENT, SYNC_FAILED_STATE, SYNC_FAIL_HANDLED_EVENT);

    public StackSyncFlowConfig() {
        super(StackSyncState.class, StackSyncEvent.class);
    }

    @Override
    public StackSyncEvent[] getEvents() {
        return StackSyncEvent.values();
    }

    @Override
    protected List<Transition<StackSyncState, StackSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackSyncState, StackSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }
}
