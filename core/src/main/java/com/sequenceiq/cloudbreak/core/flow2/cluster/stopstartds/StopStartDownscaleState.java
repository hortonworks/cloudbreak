package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

enum StopStartDownscaleState implements FlowState {

    INIT_STATE,
    STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE,
    STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE,
    STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE,
    STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE,
    STOPSTART_DOWNSCALE_FINALIZE_STATE,
    STOPSTART_DOWNSCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
