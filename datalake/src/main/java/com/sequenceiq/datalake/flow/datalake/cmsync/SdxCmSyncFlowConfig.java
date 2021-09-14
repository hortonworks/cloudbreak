package com.sequenceiq.datalake.flow.datalake.cmsync;


import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_START_EVENT;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.CORE_CM_SYNC_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.CORE_CM_SYNC_STATE;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.INIT_STATE;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.SDX_CM_SYNC_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncState.SDX_CM_SYNC_FINISHED_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class SdxCmSyncFlowConfig extends AbstractFlowConfiguration<SdxCmSyncState, SdxCmSyncEvent> {
    private static final List<Transition<SdxCmSyncState, SdxCmSyncEvent>> TRANSITIONS =
            new Builder<SdxCmSyncState, SdxCmSyncEvent>()
                    .defaultFailureEvent(SDX_CM_SYNC_FAILED_EVENT)
                    .from(INIT_STATE).to(CORE_CM_SYNC_STATE).event(SDX_CM_SYNC_START_EVENT).defaultFailureEvent()
                    .from(CORE_CM_SYNC_STATE).to(CORE_CM_SYNC_IN_PROGRESS_STATE).event(SDX_CM_SYNC_IN_PROGRESS_EVENT).defaultFailureEvent()
                    .from(CORE_CM_SYNC_IN_PROGRESS_STATE).to(SDX_CM_SYNC_FINISHED_STATE).event(SDX_CM_SYNC_FINISHED_EVENT).defaultFailureEvent()
                    .from(SDX_CM_SYNC_FINISHED_STATE).to(FINAL_STATE).event(SDX_CM_SYNC_FINALIZED_EVENT).defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<SdxCmSyncState, SdxCmSyncEvent> EDGE_CONFIG = new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE,
            SDX_CM_SYNC_FAILED_STATE, SDX_CM_SYNC_FAILED_HANDLED_EVENT);

    public SdxCmSyncFlowConfig() {
        super(SdxCmSyncState.class, SdxCmSyncEvent.class);
    }

    @Override
    protected List<Transition<SdxCmSyncState, SdxCmSyncEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<SdxCmSyncState, SdxCmSyncEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public SdxCmSyncEvent[] getEvents() {
        return SdxCmSyncEvent.values();
    }

    @Override
    public SdxCmSyncEvent[] getInitEvents() {
        return new SdxCmSyncEvent[]{
                SDX_CM_SYNC_START_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "CM sync parcels";
    }
}
