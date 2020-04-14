package com.sequenceiq.freeipa.flow.freeipa.provision;

import com.sequenceiq.flow.core.FlowState;

public enum FreeIpaProvisionState implements FlowState {
    INIT_STATE,
    FREEIPA_PROVISION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    FREEIPA_INSTALL_STATE,
    CLUSTERPROXY_UPDATE_REGISTRATION_STATE,
    FREEIPA_POST_INSTALL_STATE,
    FREEIPA_PROVISION_FINISHED_STATE,
    FINAL_STATE;
}
