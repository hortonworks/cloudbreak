package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;

public enum ClusterCreationState implements FlowState {
    INIT_STATE,
    CLUSTER_PROXY_REGISTRATION_STATE,
    CLUSTER_CREATION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    UPLOAD_RECIPES_STATE,
    BOOTSTRAPPING_PUBLIC_ENDPOINT_STATE,
    CONFIGURE_KEYTABS_STATE,
    STARTING_AMBARI_SERVICES_STATE,
    STARTING_AMBARI_STATE,
    CONFIGURE_LDAP_SSO_STATE,
    INSTALLING_CLUSTER_STATE,
    CLUSTER_CREATION_FINISHED_STATE,
    CLUSTER_PROXY_GATEWAY_REGISTRATION_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
