package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupEvent.DATABASE_BACKUP_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.DATABASE_BACKUP_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.DATABASE_BACKUP_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.DATABASE_BACKUP_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.backup.DatabaseBackupState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatabaseBackupFlowConfig extends AbstractFlowConfiguration<DatabaseBackupState, DatabaseBackupEvent>
    implements RetryableFlowConfiguration<DatabaseBackupEvent> {

    private static final List<Transition<DatabaseBackupState, DatabaseBackupEvent>> TRANSITIONS =
        new Transition.Builder<DatabaseBackupState, DatabaseBackupEvent>()
            .defaultFailureEvent(DATABASE_BACKUP_FAILED_EVENT)

            .from(INIT_STATE).to(DATABASE_BACKUP_STATE)
            .event(DATABASE_BACKUP_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_BACKUP_STATE).to(DATABASE_BACKUP_FINISHED_STATE)
            .event(DATABASE_BACKUP_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_BACKUP_FINISHED_STATE).to(FINAL_STATE)
            .event(DATABASE_BACKUP_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<DatabaseBackupState, DatabaseBackupEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATABASE_BACKUP_FAILED_STATE, DATABASE_BACKUP_FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

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
            DATABASE_BACKUP_EVENT
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

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
