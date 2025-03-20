package com.sequenceiq.datalake.flow.sku;


import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowEvent.DATALAKE_SKU_MIGRATION_FINISHED_EVENT;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowState.DATALAKE_SKU_MIGRATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowState.DATALAKE_SKU_MIGRATION_FINISED_STATE;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowState.DATALAKE_SKU_MIGRATION_STATE;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.sku.DataLakeSkuMigrationFlowState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DataLakeSkuMigrationFlowConfig
        extends AbstractFlowConfiguration<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent> {

    public static final FlowEdgeConfig<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_SKU_MIGRATION_FAILED_STATE,
                    DATALAKE_SKU_MIGRATION_FAIL_HANDLED_EVENT);

    private static final List<Transition<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent>>
            TRANSITIONS = new Transition.Builder<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent>()
                    .defaultFailureEvent(DATALAKE_SKU_MIGRATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_SKU_MIGRATION_STATE)
                    .event(DATALAKE_SKU_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_SKU_MIGRATION_STATE)
                    .to(DATALAKE_SKU_MIGRATION_FINISED_STATE)
                    .event(DATALAKE_SKU_MIGRATION_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_SKU_MIGRATION_FINISED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_SKU_MIGRATION_FINALIZED_EVENT)
                    .defaultFailureEvent()
                    .build();

    protected DataLakeSkuMigrationFlowConfig() {
        super(DataLakeSkuMigrationFlowState.class, DataLakeSkuMigrationFlowEvent.class);
    }

    @Override
    protected List<Transition<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DataLakeSkuMigrationFlowState, DataLakeSkuMigrationFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DataLakeSkuMigrationFlowEvent[] getEvents() {
        return DataLakeSkuMigrationFlowEvent.values();
    }

    @Override
    public DataLakeSkuMigrationFlowEvent[] getInitEvents() {
        return new DataLakeSkuMigrationFlowEvent[] {
                DATALAKE_SKU_MIGRATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate Skus for datalake";
    }
}