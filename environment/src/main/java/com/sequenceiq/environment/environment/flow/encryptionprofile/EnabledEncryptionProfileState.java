package com.sequenceiq.environment.environment.flow.encryptionprofile;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnabledEncryptionProfileState implements FlowState {

    INIT_STATE,
    VALIDATE_ENABLE_ENCRYPTION_PROFILE_STATE,
    SET_ENCRYPTION_PROFILE_STATE,
    UPDATE_SSL_CONFIG_FREEIPA_STATE,
    UPDATE_SSL_CONFIG_CLUSTERS_STATE,
    ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE,
    ENABLE_ENCRYPTION_PROFILE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
