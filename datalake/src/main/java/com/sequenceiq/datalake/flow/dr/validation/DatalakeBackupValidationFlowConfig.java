package com.sequenceiq.datalake.flow.dr.validation;


import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_BACKUP_VALIDATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.DATALAKE_BACKUP_VALIDATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.DATALAKE_BACKUP_VALIDATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.DATALAKE_TRIGGERING_BACKUP_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeBackupValidationFlowConfig extends AbstractFlowConfiguration<DatalakeBackupValidationState, DatalakeBackupValidationEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeBackupValidationEvent> {

    private static final List<Transition<DatalakeBackupValidationState, DatalakeBackupValidationEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeBackupValidationState, DatalakeBackupValidationEvent>()
                    .defaultFailureEvent(DATALAKE_BACKUP_VALIDATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_TRIGGERING_BACKUP_VALIDATION_STATE)
                    .event(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT).noFailureEvent()

                    .from(DATALAKE_TRIGGERING_BACKUP_VALIDATION_STATE)
                    .to(DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_STATE)
                    .event(DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_BACKUP_VALIDATION_IN_PROGRESS_STATE)
                    .to(DATALAKE_BACKUP_VALIDATION_FINISHED_STATE)
                    .event(DATALAKE_BACKUP_VALIDATION_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_BACKUP_VALIDATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_BACKUP_VALIDATION_FINALIZED_EVENT).defaultFailureEvent()

                    .from(DATALAKE_BACKUP_VALIDATION_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeBackupValidationState, DatalakeBackupValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_BACKUP_VALIDATION_FAILED_STATE, DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT);

    public DatalakeBackupValidationFlowConfig() {
        super(DatalakeBackupValidationState.class, DatalakeBackupValidationEvent.class);
    }

    @Override
    public DatalakeBackupValidationEvent[] getEvents() {
        return DatalakeBackupValidationEvent.values();
    }

    @Override
    public DatalakeBackupValidationEvent[] getInitEvents() {
        return new DatalakeBackupValidationEvent[]{
                DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Backup";
    }

    @Override
    protected List<Transition<DatalakeBackupValidationState, DatalakeBackupValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeBackupValidationState, DatalakeBackupValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeBackupValidationEvent getRetryableEvent() {
        return DATALAKE_BACKUP_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
