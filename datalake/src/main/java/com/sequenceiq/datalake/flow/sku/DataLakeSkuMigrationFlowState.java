package com.sequenceiq.datalake.flow.sku;

import com.sequenceiq.datalake.flow.FillInMemoryStateStoreRestartAction;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.RestartAction;

public enum DataLakeSkuMigrationFlowState implements FlowState {

    INIT_STATE,
    DATALAKE_SKU_MIGRATION_STATE,
    DATALAKE_SKU_MIGRATION_FINISED_STATE,
    FINAL_STATE,
    DATALAKE_SKU_MIGRATION_FAILED_STATE;

    private final Class<? extends RestartAction> restartAction = FillInMemoryStateStoreRestartAction.class;

    @Override
    public Class<? extends RestartAction> restartAction() {
        return FillInMemoryStateStoreRestartAction.class;
    }

}
