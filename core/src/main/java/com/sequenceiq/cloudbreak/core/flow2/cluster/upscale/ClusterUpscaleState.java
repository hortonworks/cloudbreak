package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

enum ClusterUpscaleState implements FlowState {
    INIT_STATE,
    UPLOAD_UPSCALE_RECIPES_STATE,
    CHECK_HOST_METADATA_STATE,
    UPSCALING_CLUSTER_MANAGER_STATE,
    UPSCALING_AMBARI_FINISHED_STATE,
    AMBARI_REGENERATE_KERBEROS_KEYTABS_STATE,
    AMBARI_GATHER_INSTALLED_COMPONENTS_STATE,
    AMBARI_STOP_COMPONENTS_STATE,
    AMBARI_STOP_SERVER_AGENT_STATE,
    AMBARI_START_SERVER_AGENT_STATE,
    AMBARI_ENSURE_COMPONENTS_ARE_STOPPED_STATE,
    AMBARI_INIT_COMPONENTS_STATE,
    AMBARI_INSTALL_COMPONENTS_STATE,
    AMBARI_START_COMPONENTS_STATE,
    AMBARI_RESTART_ALL_STATE,
    AMBARI_REPAIR_SINGLE_MASTER_FINISHED_STATE,
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
