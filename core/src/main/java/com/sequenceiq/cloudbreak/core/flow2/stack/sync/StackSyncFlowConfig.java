package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.STACK_SYNC_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent.SYNC_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncState.SYNC_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.cloudbreak.core.flow2.config.RetryableFlowConfiguration;

@Component
public class StackSyncFlowConfig extends AbstractFlowConfiguration<StackSyncState, StackSyncEvent>
    implements RetryableFlowConfiguration<StackSyncEvent> {

    private static final List<Transition<StackSyncState, StackSyncEvent>> TRANSITIONS = new Builder<StackSyncState, StackSyncEvent>()
            .defaultFailureEvent(StackSyncEvent.SYNC_FAILURE_EVENT)
            .from(INIT_STATE).to(SYNC_STATE).event(STACK_SYNC_EVENT).noFailureEvent()
            .from(SYNC_STATE).to(SYNC_FINISHED_STATE).event(SYNC_FINISHED_EVENT).defaultFailureEvent()
            .from(SYNC_FINISHED_STATE).to(FINAL_STATE).event(SYNC_FINALIZED_EVENT).defaultFailureEvent()
            .build();

    private static final FlowEdgeConfig<StackSyncState, StackSyncEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, SYNC_FAILED_STATE, SYNC_FAIL_HANDLED_EVENT);

    private static final String FLOW_DISPLAY_NAME = "Stack Sync";

    public StackSyncFlowConfig() {
        super(StackSyncState.class, StackSyncEvent.class);
    }

    @Override
    public StackSyncEvent[] getEvents() {
        return StackSyncEvent.values();
    }

    @Override
    public StackSyncEvent[] getInitEvents() {
        return new StackSyncEvent[] {
                STACK_SYNC_EVENT
        };
    }

    @Override
    protected List<Transition<StackSyncState, StackSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<StackSyncState, StackSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public String getDisplayName() {
        return FLOW_DISPLAY_NAME;
    }

    @Override
    public StackSyncEvent getFailHandledEvent() {
        return SYNC_FAIL_HANDLED_EVENT;
    }
}
