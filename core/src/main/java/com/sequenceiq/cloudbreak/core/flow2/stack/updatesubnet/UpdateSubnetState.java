package com.sequenceiq.cloudbreak.core.flow2.stack.updatesubnet;

import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;

enum UpdateSubnetState implements FlowState {
    INIT_STATE,
    UPDATE_SUBNET_STATE,
    UPDATE_SUBNET_FINISHED_STATE,
    UPDATE_SUBNET_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends AbstractAction> action() {
        return null;
    }
}
