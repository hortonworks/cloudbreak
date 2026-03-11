package com.sequenceiq.datalake.flow.datalake.kraftmigration;

import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationEvent.DATALAKE_KRAFT_MIGRATION_TRIGGER_EVENT;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.DATALAKE_KRAFT_MIGRATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.DATALAKE_KRAFT_MIGRATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.DATALAKE_KRAFT_MIGRATION_START_STATE;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.datalake.kraftmigration.DatalakeKraftMigrationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeKraftMigrationFlowConfig
        extends AbstractFlowConfiguration<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeKraftMigrationEvent> {

    private static final List<Transition<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent>()
                    .defaultFailureEvent(DATALAKE_KRAFT_MIGRATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_KRAFT_MIGRATION_START_STATE)
                    .event(DATALAKE_KRAFT_MIGRATION_TRIGGER_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_KRAFT_MIGRATION_START_STATE)
                    .to(DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_STATE)
                    .event(DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_STATE)
                    .to(DATALAKE_KRAFT_MIGRATION_FINISHED_STATE)
                    .event(DATALAKE_KRAFT_MIGRATION_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_KRAFT_MIGRATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_KRAFT_MIGRATION_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_KRAFT_MIGRATION_FAILED_STATE, DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT);

    protected DatalakeKraftMigrationFlowConfig() {
        super(DatalakeKraftMigrationState.class, DatalakeKraftMigrationEvent.class);
    }

    @Override
    protected List<Transition<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeKraftMigrationState, DatalakeKraftMigrationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeKraftMigrationEvent[] getEvents() {
        return DatalakeKraftMigrationEvent.values();
    }

    @Override
    public DatalakeKraftMigrationEvent[] getInitEvents() {
        return new DatalakeKraftMigrationEvent[]{DATALAKE_KRAFT_MIGRATION_TRIGGER_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "ZooKeeper to KRaft migration for Data Lake";
    }

    @Override
    public DatalakeKraftMigrationEvent getRetryableEvent() {
        return DATALAKE_KRAFT_MIGRATION_FAILED_HANDLED_EVENT;
    }

    @Override
    public List<DatalakeKraftMigrationEvent> getStackRetryEvents() {
        return List.of(DATALAKE_KRAFT_MIGRATION_IN_PROGRESS_EVENT);
    }
}
