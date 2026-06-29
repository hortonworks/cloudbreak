package com.sequenceiq.freeipa.flow.freeipa.migration;

import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FAILURE_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState.INIT_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState.MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState.MULTI_AZ_MIGRATION_FINALIZE_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.flow.StackStatusFinalizerAbstractFlowConfig;

@Component
public class MultiAzMigrationFinalizeFlowConfig
        extends StackStatusFinalizerAbstractFlowConfig<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent> {

    private static final List<Transition<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent>> TRANSITIONS =
            new Transition.Builder<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent>()
                    .defaultFailureEvent(MULTI_AZ_MIGRATION_FINALIZE_FAILURE_EVENT)

                    .from(INIT_STATE)
                    .to(MULTI_AZ_MIGRATION_FINALIZE_STATE)
                    .event(MULTI_AZ_MIGRATION_FINALIZE_EVENT)
                    .defaultFailureEvent()

                    .from(MULTI_AZ_MIGRATION_FINALIZE_STATE)
                    .to(FINAL_STATE)
                    .event(MULTI_AZ_MIGRATION_FINALIZE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent> EDGE_CONFIG = new FlowEdgeConfig<>(
            INIT_STATE,
            FINAL_STATE,
            MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE,
            MULTI_AZ_MIGRATION_FINALIZE_FAIL_HANDLED_EVENT);

    public MultiAzMigrationFinalizeFlowConfig() {
        super(MultiAzMigrationFinalizeState.class, MultiAzMigrationFinalizeFlowEvent.class);
    }

    @Override
    protected List<Transition<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MultiAzMigrationFinalizeState, MultiAzMigrationFinalizeFlowEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MultiAzMigrationFinalizeFlowEvent[] getEvents() {
        return MultiAzMigrationFinalizeFlowEvent.values();
    }

    @Override
    public MultiAzMigrationFinalizeFlowEvent[] getInitEvents() {
        return new MultiAzMigrationFinalizeFlowEvent[]{MULTI_AZ_MIGRATION_FINALIZE_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "FreeIPA multi-AZ migration finalization";
    }
}
