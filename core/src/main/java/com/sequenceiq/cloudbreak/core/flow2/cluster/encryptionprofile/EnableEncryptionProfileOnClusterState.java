package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnableEncryptionProfileOnClusterState implements FlowState {

    INIT_STATE,
    SET_ENCRYPTION_PROFILE_STATE,
    UPDATE_CM_POLICY_STATE,
    GENERATE_ALTERNATIVE_CERTIFICATE_STATE,
    ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FINISHED_STATE,
    ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
