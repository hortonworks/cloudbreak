package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_UPDATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeState.DISK_UPDATE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeState.DISK_UPDATE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeState.DISK_UPDATE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DiskResizeFlowConfig extends StackStatusFinalizerAbstractFlowConfig<DiskResizeState, DiskResizeEvent>
        implements RetryableFlowConfiguration<DiskResizeEvent> {

    private static final List<Transition<DiskResizeState, DiskResizeEvent>> TRANSITIONS =
            new Builder<DiskResizeState, DiskResizeEvent>()

                    .from(INIT_STATE)
                    .to(DISK_UPDATE_STATE)
                    .event(DISK_RESIZE_TRIGGER_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(DISK_UPDATE_STATE)
                    .to(DISK_UPDATE_FINISHED_STATE)
                    .event(DISK_RESIZE_FINISHED_EVENT)
                    .failureEvent(FAILURE_EVENT)

                    .from(DISK_UPDATE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(FINALIZED_EVENT)
                    .noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DiskResizeState, DiskResizeEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            DISK_UPDATE_FAILED_STATE,
            DISK_UPDATE_FAILURE_HANDLED_EVENT);

    public DiskResizeFlowConfig() {
        super(DiskResizeState.class, DiskResizeEvent.class);
    }

    @Override
    protected List<Transition<DiskResizeState, DiskResizeEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DiskResizeState, DiskResizeEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DiskResizeEvent[] getEvents() {
        return DiskResizeEvent.values();
    }

    @Override
    public DiskResizeEvent[] getInitEvents() {
        return new DiskResizeEvent[] {
                DISK_RESIZE_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Resizing disks on the stack";
    }

    @Override
    public DiskResizeEvent getRetryableEvent() {
        return DISK_UPDATE_FAILURE_HANDLED_EVENT;
    }
}
