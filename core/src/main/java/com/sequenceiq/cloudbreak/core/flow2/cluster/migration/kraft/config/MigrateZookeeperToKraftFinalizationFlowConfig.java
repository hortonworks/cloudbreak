package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class MigrateZookeeperToKraftFinalizationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftFinalizationState,
        MigrateZookeeperToKraftFinalizationStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftFinalizationStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftFinalizationState, MigrateZookeeperToKraftFinalizationStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftFinalizationState, MigrateZookeeperToKraftFinalizationStateSelectors>()
                    .defaultFailureEvent(FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)

                    .from(INIT_STATE).to(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE)
                    .event(START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE).to(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE)
                    .event(FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<MigrateZookeeperToKraftFinalizationState, MigrateZookeeperToKraftFinalizationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE,
                    HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT);

    public MigrateZookeeperToKraftFinalizationFlowConfig() {
        super(MigrateZookeeperToKraftFinalizationState.class, MigrateZookeeperToKraftFinalizationStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftFinalizationState, MigrateZookeeperToKraftFinalizationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftFinalizationState, MigrateZookeeperToKraftFinalizationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftFinalizationStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftFinalizationStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftFinalizationStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftFinalizationStateSelectors[]{
                START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Finalize Zookeeper to KRaft migration";
    }

    @Override
    public MigrateZookeeperToKraftFinalizationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
    }
}