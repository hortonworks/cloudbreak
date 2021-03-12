package com.sequenceiq.freeipa.flow.freeipa.provision;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaProvisionState implements FlowState {
    INIT_STATE,
    FREEIPA_PROVISION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    ORCHESTRATOR_CONFIG_STATE,
    VALIDATING_CLOUD_STORAGE_STATE,
    FREEIPA_INSTALL_STATE,
    CLUSTERPROXY_UPDATE_REGISTRATION_STATE,
    FREEIPA_POST_INSTALL_STATE,
    FREEIPA_PROVISION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
