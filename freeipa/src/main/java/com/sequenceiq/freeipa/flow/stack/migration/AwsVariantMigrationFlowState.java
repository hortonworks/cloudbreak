package com.sequenceiq.freeipa.flow.stack.migration;

import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;
import com.sequenceiq.freeipa.flow.FillInMemoryStateStoreRestartAction;

public enum AwsVariantMigrationFlowState implements FlowState {
    INIT_STATE,
    AWS_VARIANT_MIGRATION_FAILED_STATE,
    CREATE_RESOURCES_STATE,
    DELETE_CLOUD_FORMATION_STATE,
    CHANGE_VARIANT_STATE,
    FINAL_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return restartAction;
    }
}
