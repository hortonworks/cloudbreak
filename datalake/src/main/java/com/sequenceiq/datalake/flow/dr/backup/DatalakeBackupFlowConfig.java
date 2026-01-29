package com.sequenceiq.datalake.flow.dr.backup;

import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_CANCELLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_CANCEL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_SERVICES_STOPPED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_BACKUP_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_BACKUP_AWAIT_SERVICES_STOPPED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_BACKUP_CANCELLED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_BACKUP_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_BACKUP_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_DATABASE_BACKUP_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_DATABASE_BACKUP_START_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.DATALAKE_TRIGGERING_BACKUP_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeBackupFlowConfig extends AbstractFlowConfiguration<DatalakeBackupState, DatalakeBackupEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeBackupEvent> {

    private static final List<Transition<DatalakeBackupState, DatalakeBackupEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeBackupState, DatalakeBackupEvent>()
                    .defaultFailureEvent(DATALAKE_BACKUP_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_TRIGGERING_BACKUP_STATE)
                    .event(DATALAKE_TRIGGER_BACKUP_EVENT)
                    .noFailureEvent()

                    .from(DATALAKE_TRIGGERING_BACKUP_STATE)
                    .to(DATALAKE_BACKUP_AWAIT_SERVICES_STOPPED_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_BACKUP_AWAIT_SERVICES_STOPPED_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_START_STATE)
                    .event(DATALAKE_BACKUP_SERVICES_STOPPED_EVENT)
                    .defaultFailureEvent()

                    .from(INIT_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_START_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_EVENT)
                    .noFailureEvent()

                    .from(DATALAKE_DATABASE_BACKUP_START_STATE)
                    .to(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_COULD_NOT_START_EVENT)

                    .from(DATALAKE_DATABASE_BACKUP_COULD_NOT_START_STATE)
                    .to(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT)
                    .failureState(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(DATALAKE_DATABASE_BACKUP_IN_PROGRESS_STATE)
                    .to(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_FULL_BACKUP_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_BACKUP_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .to(DATALAKE_BACKUP_FINISHED_STATE)
                    .event(DATALAKE_BACKUP_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .to(DATALAKE_BACKUP_CANCELLED_STATE)
                    .event(DATALAKE_BACKUP_CANCELLED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_BACKUP_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_BACKUP_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_BACKUP_CANCELLED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_BACKUP_CANCEL_HANDLED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_DATABASE_BACKUP_FAILED_STATE)
                    .to(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT)
                    .failureState(DATALAKE_FULL_BACKUP_IN_PROGRESS_STATE)
                    .failureEvent(DATALAKE_DATABASE_BACKUP_FAILED_EVENT)

                    .from(DATALAKE_BACKUP_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_BACKUP_FAILURE_HANDLED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeBackupState, DatalakeBackupEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_BACKUP_FAILED_STATE, DATALAKE_BACKUP_FAILURE_HANDLED_EVENT);

    public DatalakeBackupFlowConfig() {
        super(DatalakeBackupState.class, DatalakeBackupEvent.class);
    }

    @Override
    public DatalakeBackupEvent[] getEvents() {
        return DatalakeBackupEvent.values();
    }

    @Override
    public DatalakeBackupEvent[] getInitEvents() {
        return new DatalakeBackupEvent[]{
            DATALAKE_DATABASE_BACKUP_EVENT,
            DATALAKE_TRIGGER_BACKUP_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Backup";
    }

    @Override
    protected List<Transition<DatalakeBackupState, DatalakeBackupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeBackupState, DatalakeBackupEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeBackupEvent getRetryableEvent() {
        return DATALAKE_DATABASE_BACKUP_FAILURE_HANDLED_EVENT;
    }
}
