package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.FULL_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.FULL_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_IN_PROGRESS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.RESTORE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.RESTORE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.RESTORE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.RESTORE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.DATABASE_RESTORE_IN_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.INIT_STATE;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DatabaseRestoreFlowConfig extends AbstractFlowConfiguration<DatabaseRestoreState, DatabaseRestoreEvent>
    implements RetryableFlowConfiguration<DatabaseRestoreEvent> {

    private static final List<Transition<DatabaseRestoreState, DatabaseRestoreEvent>> TRANSITIONS =
        new Transition.Builder<DatabaseRestoreState, DatabaseRestoreEvent>()
            .defaultFailureEvent(DATABASE_RESTORE_FAILED_EVENT)

            .from(INIT_STATE).to(DATABASE_RESTORE_IN_PROGRESS_STATE)
            .event(DATABASE_RESTORE_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_RESTORE_IN_PROGRESS_STATE).to(FULL_RESTORE_IN_PROGRESS_STATE)
            .event(FULL_RESTORE_IN_PROGRESS_EVENT)
            .defaultFailureEvent()

            .from(FULL_RESTORE_IN_PROGRESS_STATE).to(RESTORE_FINISHED_STATE)
            .event(RESTORE_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(RESTORE_FINISHED_STATE).to(FINAL_STATE)
            .event(RESTORE_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<DatabaseRestoreState, DatabaseRestoreEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, RESTORE_FAILED_STATE, RESTORE_FAIL_HANDLED_EVENT);

    public DatabaseRestoreFlowConfig() {
        super(DatabaseRestoreState.class, DatabaseRestoreEvent.class);
    }

    @Override
    protected List<Transition<DatabaseRestoreState, DatabaseRestoreEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<DatabaseRestoreState, DatabaseRestoreEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public DatabaseRestoreEvent[] getEvents() {
        return DatabaseRestoreEvent.values();
    }

    @Override
    public DatabaseRestoreEvent[] getInitEvents() {
        return new DatabaseRestoreEvent[]{
            DATABASE_RESTORE_IN_PROGRESS_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Restore SDX database";
    }

    @Override
    public DatabaseRestoreEvent getRetryableEvent() {
        return DATABASE_RESTORE_FAILED_EVENT;
    }
}
