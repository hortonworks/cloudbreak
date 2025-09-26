package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;

import java.util.List;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

public class MigrateZookeeperToKraftValidationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftValidationState,
        MigrateZookeeperToKraftValidationStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftValidationStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftValidationState, MigrateZookeeperToKraftValidationStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftValidationState, MigrateZookeeperToKraftValidationStateSelectors>()
                    .defaultFailureEvent(MigrateZookeeperToKraftValidationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT)

                    .from(INIT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FINISHED_STATE)
                    .event(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<MigrateZookeeperToKraftValidationState, MigrateZookeeperToKraftValidationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_FAILED_STATE,
                    HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT);

    public MigrateZookeeperToKraftValidationFlowConfig() {
        super(MigrateZookeeperToKraftValidationState.class, MigrateZookeeperToKraftValidationStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftValidationState, MigrateZookeeperToKraftValidationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftValidationState, MigrateZookeeperToKraftValidationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftValidationStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftValidationStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftValidationStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftValidationStateSelectors[]{
                START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate Zookeeper to KRaft validation";
    }

    @Override
    public MigrateZookeeperToKraftValidationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
    }
}
