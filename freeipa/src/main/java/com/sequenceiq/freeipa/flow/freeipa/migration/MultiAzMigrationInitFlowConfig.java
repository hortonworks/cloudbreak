package com.sequenceiq.freeipa.flow.freeipa.migration;

import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_RESULT_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState.MULTI_AZ_MIGRATION_INIT_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState.MULTI_AZ_MIGRATION_INIT_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState.MULTI_AZ_MIGRATION_INIT_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class MultiAzMigrationInitFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent> {

    private static final List<Transition<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent>> TRANSITIONS =
            new Transition.Builder<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent>()
                    .defaultFailureEvent(MULTI_AZ_MIGRATION_INIT_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(MULTI_AZ_MIGRATION_INIT_STATE)
                    .event(MULTI_AZ_MIGRATION_INIT_EVENT)
                    .defaultFailureEvent()

                    .from(MULTI_AZ_MIGRATION_INIT_STATE)
                    .to(MULTI_AZ_MIGRATION_INIT_FINISHED_STATE)
                    .event(MULTI_AZ_MIGRATION_INIT_RESULT_EVENT)
                    .defaultFailureEvent()

                    .from(MULTI_AZ_MIGRATION_INIT_FINISHED_STATE)
                    .to(FINAL_STATE)
                    .event(MULTI_AZ_MIGRATION_INIT_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            MULTI_AZ_MIGRATION_INIT_FAILED_STATE,
            MULTI_AZ_MIGRATION_INIT_FAIL_HANDLED_EVENT);

    public MultiAzMigrationInitFlowConfig() {
        super(MultiAzMigrationInitState.class, MultiAzMigrationInitFlowEvent.class);
    }

    @Override
    protected List<Transition<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MultiAzMigrationInitState, MultiAzMigrationInitFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MultiAzMigrationInitFlowEvent[] getEvents() {
        return MultiAzMigrationInitFlowEvent.values();
    }

    @Override
    public MultiAzMigrationInitFlowEvent[] getInitEvents() {
        return new MultiAzMigrationInitFlowEvent[]{MULTI_AZ_MIGRATION_INIT_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "FreeIPA multi-AZ migration initialization";
    }
}
