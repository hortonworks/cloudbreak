package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

enum StopStartUpscaleState implements FlowState {

    INIT_STATE,
    STOPSTART_UPSCALE_START_INSTANCE_STATE,
    STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE,
    STOPSTART_UPSCALE_FINALIZE_STATE,
    STOPSTART_UPSCALE_FAILED_STATE,
    STOPSTART_UPSCALE_HOSTS_COMMISSION_FAILED_STATE,
    FINAL_STATE;


    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
