package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_PROCESS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.DISK_SYNC_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DiskSyncFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DiskSyncState, DiskSyncEvent>
        implements RetryableFlowConfiguration<DiskSyncEvent> {

    private static final List<Transition<DiskSyncState, DiskSyncEvent>> TRANSITIONS =
            new Builder<DiskSyncState, DiskSyncEvent>()

                    .from(INIT_STATE)
                    .to(DISK_SYNC_INIT_STATE)
                    .event(DISK_SYNC_TRIGGER_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(DISK_SYNC_INIT_STATE)
                    .to(DISK_SYNC_FINISHED_STATE)
                    .event(DISK_SYNC_PROCESS_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(DISK_SYNC_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DiskSyncState, DiskSyncEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            DISK_SYNC_FAILED_STATE,
            DISK_SYNC_FAILURE_HANDLED_EVENT);

    public DiskSyncFlowConfig() {
        super(DiskSyncState.class, DiskSyncEvent.class);
    }

    @Override
    protected List<Transition<DiskSyncState, DiskSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DiskSyncState, DiskSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DiskSyncEvent[] getEvents() {
        return DiskSyncEvent.values();
    }

    @Override
    public DiskSyncEvent[] getInitEvents() {
        return new DiskSyncEvent[] {
                DISK_SYNC_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Synchronizing disk metadata on the stack";
    }

    @Override
    public DiskSyncEvent getRetryableEvent() {
        return DISK_SYNC_FAILURE_HANDLED_EVENT;
    }
}
