package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.core.flow2.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterCreationState implements FlowState {
    INIT_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    MOUNT_DISKS_STATE,
    UPLOAD_RECIPES_STATE,
    STARTING_AMBARI_SERVICES_STATE,
    STARTING_AMBARI_STATE,
    CONFIGURE_LDAP_SSO_STATE,
    INSTALLING_CLUSTER_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
