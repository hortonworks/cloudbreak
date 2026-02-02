package com.sequenceiq.freeipa.flow.freeipa.loadbalancer;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum FreeIpaLoadBalancerProvisionState implements FlowState {
    INIT_STATE,
    PROVISION_FAILED_STATE,
    CREATE_CONFIGURATION_STATE,
    PROVISIONING_STATE,
    METADATA_COLLECTION_STATE,
    LOAD_BALANCER_DOMAIN_UPDATE_STATE,
    LOAD_BALANCER_CREATION_FINISHED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
