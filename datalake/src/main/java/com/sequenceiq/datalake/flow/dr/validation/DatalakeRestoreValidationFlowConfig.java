package com.sequenceiq.datalake.flow.dr.validation;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.DATALAKE_RESTORE_VALIDATION_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.DATALAKE_RESTORE_VALIDATION_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.DATALAKE_TRIGGERING_RESTORE_VALIDATION_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class DatalakeRestoreValidationFlowConfig extends AbstractFlowConfiguration<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeRestoreValidationEvent> {

    private static final List<Transition<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent>> TRANSITIONS =
            new Builder<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent>()
                    .defaultFailureEvent(DATALAKE_RESTORE_VALIDATION_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_TRIGGERING_RESTORE_VALIDATION_STATE)
                    .event(DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT).noFailureEvent()

                    .from(DATALAKE_TRIGGERING_RESTORE_VALIDATION_STATE)
                    .to(DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_STATE)
                    .event(DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_RESTORE_VALIDATION_IN_PROGRESS_STATE)
                    .to(DATALAKE_RESTORE_VALIDATION_FINISHED_STATE)
                    .event(DATALAKE_RESTORE_VALIDATION_SUCCESS_EVENT).defaultFailureEvent()

                    .from(DATALAKE_RESTORE_VALIDATION_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_RESTORE_VALIDATION_FINALIZED_EVENT).defaultFailureEvent()

                    .from(DATALAKE_RESTORE_VALIDATION_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT).noFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_RESTORE_VALIDATION_FAILED_STATE, DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT);

    public DatalakeRestoreValidationFlowConfig() {
        super(DatalakeRestoreValidationState.class, DatalakeRestoreValidationEvent.class);
    }

    @Override
    public DatalakeRestoreValidationEvent[] getEvents() {
        return DatalakeRestoreValidationEvent.values();
    }

    @Override
    public DatalakeRestoreValidationEvent[] getInitEvents() {
        return new DatalakeRestoreValidationEvent[]{
                DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Restore Validation";
    }

    @Override
    protected List<Transition<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeRestoreValidationState, DatalakeRestoreValidationEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeRestoreValidationEvent getRetryableEvent() {
        return DATALAKE_RESTORE_VALIDATION_FAILURE_HANDLED_EVENT;
    }
}
