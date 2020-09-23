package com.sequenceiq.datalake.flow.dr.backup;


import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_DATABASE_BACKUP_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupEvent.DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_DATABASE_BACKUP_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_DATABASE_BACKUP_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_DATABASE_BACKUP_START_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeDatabaseBackupState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeDatabaseBackupFlowConfig extends AbstractFlowConfiguration<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent>
        implements RetryableFlowConfiguration<DatalakeDatabaseBackupEvent> {

    private static final List<Transition<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent>()
                    .defaultFailureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_START_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_EVENT).noFailureEvent()

                    .from(DATALAKE_DATABASE_BACKUP_START_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT)

                    .from(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE)
                    .to(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_BACKUP_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_FINISHED_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_SUCCESS_EVENT)
                    .failureState(DATALAKE_DATABASE_BACKUP_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(DATALAKE_DATABASE_BACKUP_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_FINALIZED_EVENT).defaultFailureEvent()

                    .from(DATALAKE_DATABASE_BACKUP_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_DATABASE_BACKUP_FAILED_STATE, DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT);

    public DatalakeDatabaseBackupFlowConfig() {
        super(DatalakeDatabaseBackupState.class, DatalakeDatabaseBackupEvent.class);
    }

    @Override
    public DatalakeDatabaseBackupEvent[] getEvents() {
        return DatalakeDatabaseBackupEvent.values();
    }

    @Override
    public DatalakeDatabaseBackupEvent[] getInitEvents() {
        return new DatalakeDatabaseBackupEvent[]{
            DATALAKE_DATABASE_BACKUP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Backup";
    }

    @Override
    protected List<Transition<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatalakeDatabaseBackupState, DatalakeDatabaseBackupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeDatabaseBackupEvent getRetryableEvent() {
        return DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
    }
}
