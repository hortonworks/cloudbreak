package com.sequenceiq.cloudbreak.core.flow2.stack.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.DisableOnGCPRestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum StackUpscaleState implements FlowState {
    INIT_STATE,
    UPSCALE_FAILED_STATE,
    UPSCALE_PREVALIDATION_STATE,
    ADD_INSTANCES_STATE(DisableOnGCPRestartAction.class),
    ADD_INSTANCES_FINISHED_STATE,
    EXTEND_METADATA_STATE,
    EXTEND_METADATA_FINISHED_STATE,
    GATEWAY_TLS_SETUP_STATE,
    BOOTSTRAP_NEW_NODES_STATE,
    EXTEND_HOST_METADATA_STATE,
    EXTEND_HOST_METADATA_FINISHED_STATE,
    MOUNT_DISKS_ON_NEW_HOSTS_STATE,
    FINAL_STATE;

    private Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    StackUpscaleState() {

    }

    StackUpscaleState(Class<? extends RestartAction> restartAction) {
        this.restartAction = restartAction;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
