package com.sequenceiq.environment.environment.flow.creation;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvCreationState implements FlowState {
    INIT_STATE,
    ENVIRONMENT_INITIALIZATION_STATE,
    ENVIRONMENT_CREATION_VALIDATION_STATE,
    COMPUTE_CLUSTER_CREATION_STARTED_STATE,
    STORAGE_CONSUMPTION_COLLECTION_SCHEDULING_STARTED_STATE,
    NETWORK_CREATION_STARTED_STATE,
    PUBLICKEY_CREATION_STARTED_STATE,
    ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_STARTED_STATE,
    FREEIPA_CREATION_STARTED_STATE,
    COMPUTE_CLUSTER_CREATION_WAITING_STATE,
    DISTRIBUTION_LIST_CREATION_STATE,
    ENV_CREATION_FINISHED_STATE,
    ENV_CREATION_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }
}
