package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_FINISHED_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncEvent.CM_SYNC_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncState.CM_SYNC_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncState.CM_SYNC_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.CmSyncState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;

@Component
public class CmSyncFlowConfig extends AbstractFlowConfiguration<CmSyncState, CmSyncEvent> {
    private static final List<Transition<CmSyncState, CmSyncEvent>> TRANSITIONS =
            new Builder<CmSyncState, CmSyncEvent>()
                    .defaultFailureEvent(CM_SYNC_FAILURE_EVENT)
                    .from(INIT_STATE).to(CM_SYNC_STATE).event(CM_SYNC_TRIGGER_EVENT)
                        .defaultFailureEvent()
                    .from(CM_SYNC_STATE).to(CmSyncState.CM_SYNC_FINISHED_STATE).event(CmSyncEvent.CM_SYNC_FINISHED_EVENT)
                        .failureEvent(CM_SYNC_FINISHED_FAILURE_EVENT)
                    .from(CmSyncState.CM_SYNC_FINISHED_STATE).to(FINAL_STATE).event(CM_SYNC_FINALIZED_EVENT)
                        .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<CmSyncState, CmSyncEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            CM_SYNC_FAILED_STATE, CM_SYNC_FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

    public CmSyncFlowConfig() {
        super(CmSyncState.class, CmSyncEvent.class);
    }

    @Override
    protected List<Transition<CmSyncState, CmSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<CmSyncState, CmSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public CmSyncEvent[] getEvents() {
        return CmSyncEvent.values();
    }

    @Override
    public CmSyncEvent[] getInitEvents() {
        return new CmSyncEvent[]{
                CM_SYNC_TRIGGER_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Sync cluster";
    }

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
