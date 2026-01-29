package com.sequenceiq.datalake.flow.dr.restore;

import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_FULL_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_SERVICES_STOPPED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_RESTORE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_DATABASE_RESTORE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_DATABASE_RESTORE_START_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_RESTORE_AWAIT_SERVICES_STOPPED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_RESTORE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_RESTORE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.DATALAKE_TRIGGERING_RESTORE_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.datalake.flow.RetryableDatalakeFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Component
public class DatalakeRestoreFlowConfig extends AbstractFlowConfiguration<DatalakeRestoreState, DatalakeRestoreEvent>
        implements RetryableDatalakeFlowConfiguration<DatalakeRestoreEvent> {

    private static final List<Transition<DatalakeRestoreState, DatalakeRestoreEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeRestoreState, DatalakeRestoreEvent>()
                    .defaultFailureEvent(DATALAKE_RESTORE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_TRIGGERING_RESTORE_STATE)
                    .event(DATALAKE_TRIGGER_RESTORE_EVENT).noFailureEvent()

                    .from(DATALAKE_TRIGGERING_RESTORE_STATE)
                    .to(DATALAKE_RESTORE_AWAIT_SERVICES_STOPPED_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_RESTORE_AWAIT_SERVICES_STOPPED_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .event(DATALAKE_RESTORE_SERVICES_STOPPED_EVENT)
                    .defaultFailureEvent()

                    .from(INIT_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_EVENT).noFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE)
                    .to(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT)
                    .failureState(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .to(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_FULL_RESTORE_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .to(DATALAKE_RESTORE_FINISHED_STATE)
                    .event(DATALAKE_RESTORE_SUCCESS_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_RESTORE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_RESTORE_FINALIZED_EVENT)
                    .defaultFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .to(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT)
                    .failureState(DATALAKE_FULL_RESTORE_IN_PROGRESS_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(DATALAKE_RESTORE_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_RESTORE_FAILURE_HANDLED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeRestoreState, DatalakeRestoreEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_RESTORE_FAILED_STATE, DATALAKE_RESTORE_FAILURE_HANDLED_EVENT);

    public DatalakeRestoreFlowConfig() {
        super(DatalakeRestoreState.class, DatalakeRestoreEvent.class);
    }

    @Override
    public DatalakeRestoreEvent[] getEvents() {
        return DatalakeRestoreEvent.values();
    }

    @Override
    public DatalakeRestoreEvent[] getInitEvents() {
        return new DatalakeRestoreEvent[]{
                DATALAKE_DATABASE_RESTORE_EVENT,
                DATALAKE_TRIGGER_RESTORE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Restore";
    }

    @Override
    protected List<Transition<DatalakeRestoreState, DatalakeRestoreEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<DatalakeRestoreState, DatalakeRestoreEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeRestoreEvent getRetryableEvent() {
        return DATALAKE_RESTORE_FAILURE_HANDLED_EVENT;
    }
}