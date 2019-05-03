package com.sequenceiq.freeipa.flow.freeipa.provision;

import com.sequenceiq.cloudbreak.core.flow2.FlowState;

public enum FreeIpaProvisionState implements FlowState {
    INIT_STATE,
    FREEIPA_PROVISION_FAILED_STATE,
    BOOTSTRAPPING_MACHINES_STATE,
    COLLECTING_HOST_METADATA_STATE,
    FREEIPA_INSTALL_STATE,
    FREEIPA_PROVISION_FINISHED_STATE,
    FINAL_STATE;
}
