package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum ClusterUpscaleState implements FlowState {
    INIT_STATE,
    UPLOAD_UPSCALE_RECIPES_STATE,
    UPSCALE_PREFLIGHT_CHECK_STATE,
    CHECK_HOST_METADATA_STATE,
    RECONFIGURE_KEYTABS_STATE,
    UPSCALING_CLUSTER_MANAGER_STATE,
    UPSCALING_CLUSTER_MANAGER_FINISHED_STATE,
    CLUSTER_MANAGER_REGENERATE_KERBEROS_KEYTABS_STATE,
    CLUSTER_MANAGER_GATHER_INSTALLED_COMPONENTS_STATE,
    CLUSTER_MANAGER_STOP_COMPONENTS_STATE,
    CLUSTER_MANAGER_STOP_SERVER_AGENT_STATE,
    CLUSTER_MANAGER_START_SERVER_AGENT_STATE,
    CLUSTER_MANAGER_ENSURE_COMPONENTS_ARE_STOPPED_STATE,
    CLUSTER_MANAGER_INIT_COMPONENTS_STATE,
    CLUSTER_MANAGER_INSTALL_COMPONENTS_STATE,
    CLUSTER_MANAGER_START_COMPONENTS_STATE,
    CLUSTER_MANAGER_RESTART_ALL_STATE,
    CLUSTER_MANAGER_REPAIR_SINGLE_MASTER_FINISHED_STATE,
    EXECUTING_POSTRECIPES_STATE,
    FINALIZE_UPSCALE_STATE,
    CLUSTER_UPSCALE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
