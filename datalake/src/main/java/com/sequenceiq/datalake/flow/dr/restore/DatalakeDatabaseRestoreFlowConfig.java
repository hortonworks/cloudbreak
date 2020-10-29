package com.sequenceiq.datalake.flow.dr.restore;


import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreEvent.DATALAKE_DATABASE_RESTORE_SUCCESS_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.DATALAKE_DATABASE_RESTORE_FAILED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.DATALAKE_DATABASE_RESTORE_FINISHED_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.DATALAKE_DATABASE_RESTORE_START_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.FINAL_STATE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeDatabaseRestoreState.INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatalakeDatabaseRestoreFlowConfig extends AbstractFlowConfiguration<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent>
        implements RetryableFlowConfiguration<DatalakeDatabaseRestoreEvent> {

    private static final List<Transition<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent>> TRANSITIONS =
            new Transition.Builder<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent>()
                    .defaultFailureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(INIT_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_EVENT).noFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_START_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_COULD_NOT_START_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_IN_PROGRESS_STATE)
                    .to(DATALAKE_DATABASE_RESTORE_FINISHED_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_SUCCESS_EVENT)
                    .failureState(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .failureEvent(DATALAKE_DATABASE_RESTORE_FAILED_EVENT)

                    .from(DATALAKE_DATABASE_RESTORE_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FINALIZED_EVENT).defaultFailureEvent()

                    .from(DATALAKE_DATABASE_RESTORE_FAILED_STATE)
                    .to(FINAL_STATE)
                    .event(DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT).defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATALAKE_DATABASE_RESTORE_FAILED_STATE, DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT);

    public DatalakeDatabaseRestoreFlowConfig() {
        super(DatalakeDatabaseRestoreState.class, DatalakeDatabaseRestoreEvent.class);
    }

    @Override
    public DatalakeDatabaseRestoreEvent[] getEvents() {
        return DatalakeDatabaseRestoreEvent.values();
    }

    @Override
    public DatalakeDatabaseRestoreEvent[] getInitEvents() {
        return new DatalakeDatabaseRestoreEvent[]{
                DATALAKE_DATABASE_RESTORE_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "SDX Database Restore";
    }

    @Override
    protected List<Transition<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatalakeDatabaseRestoreState, DatalakeDatabaseRestoreEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatalakeDatabaseRestoreEvent getRetryableEvent() {
        return DATALAKE_DATABASE_RESTORE_FAILURE_HANDLED_EVENT;
    }
}