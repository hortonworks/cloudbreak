package com.sequenceiq.datalake.flow.sku;

import com.sequenceiq.datalake.flow.sku.handler.WaitDataLakeSkuMigrationResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum DataLakeSkuMigrationFlowEvent implements FlowEvent {

    DATALAKE_SKU_MIGRATION_EVENT,
    DATALAKE_SKU_MIGRATION_FINISHED_EVENT(EventSelectorUtil.selector(WaitDataLakeSkuMigrationResult.class)),
    DATALAKE_SKU_MIGRATION_FINALIZED_EVENT,
    DATALAKE_SKU_MIGRATION_FAIL_HANDLED_EVENT,
    DATALAKE_SKU_MIGRATION_FAILED_EVENT(EventSelectorUtil.selector(DataLakeSkuMigrationFailedEvent.class));

    private final String event;

    DataLakeSkuMigrationFlowEvent() {
        event = name();
    }

    DataLakeSkuMigrationFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
