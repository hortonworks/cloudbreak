package com.sequenceiq.environment.environment.flow.loadbalancer;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum  LoadBalancerUpdateState implements FlowState {
    INIT_STATE,
    ENVIRONMENT_UPDATE_STATE,
    STACK_UPDATE_STATE,
    LOAD_BALANCER_UPDATE_FINISHED_STATE,
    LOAD_BALANCER_UPDATE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
