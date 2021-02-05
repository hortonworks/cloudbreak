package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StackLoadBalancerUpdateState implements FlowState {
    INIT_STATE,
    CREATING_LOAD_BALANCER_ENTITY_STATE,
    CREATING_CLOUD_LOAD_BALANCERS_STATE,
    COLLECTING_LOAD_BALANCER_METADATA_STATE,
    REGISTERING_PUBLIC_DNS_STATE,
    REGISTERING_FREEIPA_DNS_STATE,
    UPDATING_SERVICE_CONFIG_STATE,
    RESTARTING_CM_STATE,
    LOAD_BALANCER_UPDATE_FINISHED_STATE,
    LOAD_BALANCER_UPDATE_FAILED_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
