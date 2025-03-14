package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import com.sequenceiq.cloudbreak.core.flow2.restart.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum SkuMigrationFlowState implements FlowState {

    INIT_STATE,
    SKU_MIGRATION_CHECK_SKU_STATE,
    SKU_MIGRATION_DETACH_PUBLIC_IPS_STATE,
    SKU_MIGRATION_REMOVE_LOAD_BALANCER_STATE,
    SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_STATE,
    SKU_MIGRATION_UPDATE_DNS_STATE,
    SKU_MIGRATION_FINISHED_STATE,
    FINAL_STATE,
    SKU_MIGRATION_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }
}
