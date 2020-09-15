package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.BACKUP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.BACKUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.FULL_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.BACKUP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.BACKUP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.DATABASE_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.FULL_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.INIT_STATE;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DatabaseBackupFlowConfig extends AbstractFlowConfiguration<DatabaseBackupState, DatabaseBackupEvent>
    implements RetryableFlowConfiguration<DatabaseBackupEvent> {

    private static final List<Transition<DatabaseBackupState, DatabaseBackupEvent>> TRANSITIONS =
        new Transition.Builder<DatabaseBackupState, DatabaseBackupEvent>()
            .defaultFailureEvent(DATABASE_BACKUP_FAILED_EVENT)

            .from(INIT_STATE).to(DATABASE_BACKUP_IN_PROGRESS_STATE)
            .event(DATABASE_BACKUP_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_BACKUP_IN_PROGRESS_STATE).to(FULL_BACKUP_IN_PROGRESS_STATE)
            .event(FULL_BACKUP_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(FULL_BACKUP_IN_PROGRESS_STATE).to(BACKUP_FINISHED_STATE)
            .event(BACKUP_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(BACKUP_FINISHED_STATE).to(FINAL_STATE)
            .event(BACKUP_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<DatabaseBackupState, DatabaseBackupEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, BACKUP_FAILED_STATE, BACKUP_FAIL_HANDLED_EVENT);

    public DatabaseBackupFlowConfig() {
        super(DatabaseBackupState.class, DatabaseBackupEvent.class);
    }

    @Override
    protected List<Transition<DatabaseBackupState, DatabaseBackupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatabaseBackupState, DatabaseBackupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatabaseBackupEvent[] getEvents() {
        return DatabaseBackupEvent.values();
    }

    @Override
    public DatabaseBackupEvent[] getInitEvents() {
        return new DatabaseBackupEvent[]{
            DATABASE_BACKUP_IN_PROGRESS_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Backup SDX database";
    }

    @Override
    public DatabaseBackupEvent getRetryableEvent() {
        return DATABASE_BACKUP_FAILED_EVENT;
    }
}
