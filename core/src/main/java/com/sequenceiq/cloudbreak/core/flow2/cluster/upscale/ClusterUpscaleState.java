package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum ClusterUpscaleState implements FlowState {

    INIT_STATE,
    ADD_CLUSTER_CONTAINERS_STATE(AddClusterContainersAction.class),
    INSTALL_FS_RECIPES_STATE,
    WAIT_FOR_AMBARI_HOSTS_STATE,
    CONFIGURE_SSSD_STATE,
    INSTALL_RECIPES_STATE,
    EXECUTE_PRE_RECIPES_STATE,
    INSTALL_SERVICES_STATE,
    EXECUTE_POST_RECIPES_STATE,
    UPDATE_METADATA_STATE,
    FINALIZE_UPSCALE_STATE,
    CLUSTER_UPSCALE_FAILED_STATE,
    FINAL_STATE;

    private Class<? extends AbstractAction> clazz;

    ClusterUpscaleState() {
    }

    ClusterUpscaleState(Class<? extends AbstractAction> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<? extends AbstractAction> action() {
        return clazz;
    }
}
