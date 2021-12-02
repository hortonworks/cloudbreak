package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreEvent.DATABASE_RESTORE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.DATABASE_RESTORE_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.DATABASE_RESTORE_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.DATABASE_RESTORE_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.dr.restore.DatabaseRestoreState.INIT_STATE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizer;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowFinalizerCallback;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class DatabaseRestoreFlowConfig extends AbstractFlowConfiguration<DatabaseRestoreState, DatabaseRestoreEvent>
    implements RetryableFlowConfiguration<DatabaseRestoreEvent> {

    private static final List<Transition<DatabaseRestoreState, DatabaseRestoreEvent>> TRANSITIONS =
        new Transition.Builder<DatabaseRestoreState, DatabaseRestoreEvent>()
            .defaultFailureEvent(DATABASE_RESTORE_FAILED_EVENT)

            .from(INIT_STATE).to(DATABASE_RESTORE_STATE)
            .event(DATABASE_RESTORE_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_RESTORE_STATE).to(DATABASE_RESTORE_FINISHED_STATE)
            .event(DATABASE_RESTORE_FINISHED_EVENT)
            .defaultFailureEvent()

            .from(DATABASE_RESTORE_FINISHED_STATE).to(FINAL_STATE)
            .event(DATABASE_RESTORE_FINALIZED_EVENT)
            .defaultFailureEvent()

            .build();

    private static final FlowEdgeConfig<DatabaseRestoreState, DatabaseRestoreEvent> EDGE_CONFIG =
        new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, DATABASE_RESTORE_FAILED_STATE, DATABASE_RESTORE_FAIL_HANDLED_EVENT);

    @Inject
    private StackStatusFinalizer stackStatusFinalizer;

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
            DATABASE_RESTORE_EVENT
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

    @Override
    public FlowFinalizerCallback getFinalizerCallBack() {
        return stackStatusFinalizer;
    }
}
