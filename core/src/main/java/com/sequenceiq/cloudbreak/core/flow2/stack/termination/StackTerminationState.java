package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum StackTerminationState implements FlowState {
    INIT_STATE,
    PRE_TERMINATION_STATE(StackPreTerminationAction.class),
    DELETE_USERDATA_SECRETS_STATE(DeleteUserdataSecretsAction.class),
    CLUSTER_PROXY_DEREGISTER_STATE(ClusterProxyDeregisterAction.class),
    CCM_KEY_DEREGISTER_STATE(CcmKeyDeregisterAction.class),
    ATTACHED_VOLUME_CONSUMPTION_COLLECTION_UNSCHEDULING_STATE(AttachedVolumeConsumptionCollectionUnschedulingAction.class),
    TERMINATION_STATE(StackTerminationAction.class),
    TERMINATION_FAILED_STATE(StackTerminationFailureAction.class),
    TERMINATION_FINISHED_STATE(StackTerminationFinishedAction.class),
    FINAL_STATE;

    private Class<? extends AbstractStackAction<?, ?, ?, ?>> action;

    StackTerminationState() {
    }

    StackTerminationState(Class<? extends AbstractStackAction<?, ?, ?, ?>> action) {
        this.action = action;
    }

    @Override
    public Class<? extends AbstractStackAction<?, ?, ?, ?>> action() {
        return action;
    }

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
