package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum ClusterUpscaleState implements FlowState<ClusterUpscaleState, ClusterUpscaleEvent> {

    INIT_STATE,
    ADD_CLUSTER_CONTAINERS_STATE,
    INSTALL_FS_RECIPES_STATE,
    WAIT_FOR_AMBARI_HOSTS_STATE,
    CONFIGURE_SSSD_STATE,
    INSTALL_RECIPES_STATE,
    EXECUTE_PRE_RECIPES_STATE,
    INSTALL_SERVICES_STATE,
    EXECUTE_POST_RECIPES_STATE,
    UPDATE_METADATA_STATE,
    FINALIZE_STATE,
    FAILED_STATE,
    FINAL_STATE;

    private Class<?> clazz;

    ClusterUpscaleState() {
    }

    ClusterUpscaleState(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<?> action() {
        return clazz;
    }
}
