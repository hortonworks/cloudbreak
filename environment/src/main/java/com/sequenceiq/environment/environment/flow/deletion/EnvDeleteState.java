package com.sequenceiq.environment.environment.flow.deletion;

import com.sequenceiq.environment.environment.flow.EnvironmentFillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum EnvDeleteState implements FlowState {

    INIT_STATE,
    FREEIPA_DELETE_STARTED_STATE,
    STORAGE_CONSUMPTION_COLLECTION_UNSCHEDULING_STARTED_STATE,
    RDBMS_DELETE_STARTED_STATE,
    ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_STARTED_STATE,
    PUBLICKEY_DELETE_STARTED_STATE,
    NETWORK_DELETE_STARTED_STATE,
    IDBROKER_MAPPINGS_DELETE_STARTED_STATE,
    S3GUARD_TABLE_DELETE_STARTED_STATE,
    CLUSTER_DEFINITION_DELETE_STARTED_STATE,
    UMS_RESOURCE_DELETE_STARTED_STATE,
    EVENT_CLEANUP_STARTED_STATE,
    DISTRIBUTION_LIST_DELETE_STATE,
    ENV_DELETE_FINISHED_STATE,
    ENV_DELETE_FAILED_STATE,
    FINAL_STATE;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return EnvironmentFillInMemoryStateStoreRestartAction.class;
    }

}
