package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DisableEncryptionProfileOnClusterState implements FlowState {

    INIT_STATE,
    UNSET_ENCRYPTION_PROFILE_STATE,
    DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE,
    DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
