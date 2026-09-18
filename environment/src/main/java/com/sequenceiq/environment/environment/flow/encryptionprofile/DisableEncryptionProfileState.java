package com.sequenceiq.environment.environment.flow.encryptionprofile;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DisableEncryptionProfileState implements FlowState {

    INIT_STATE,
    UNSET_ENCRYPTION_PROFILE_STATE,
    DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_STATE,
    DISABLE_ENCRYPTION_PROFILE_ON_STACKS_STATE,
    DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE,
    DISABLE_ENCRYPTION_PROFILE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
