package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINALIZE_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class MigrateZookeeperToKraftRollbackFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftRollbackState,
        MigrateZookeeperToKraftRollbackStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftRollbackStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftRollbackState, MigrateZookeeperToKraftRollbackStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftRollbackState, MigrateZookeeperToKraftRollbackStateSelectors>()
                    .defaultFailureEvent(FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)

                    .from(INIT_STATE).to(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_STATE)
                    .event(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_STATE).to(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE)
                    .event(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_STATE).to(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE)
                    .event(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()

                    .from(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT)
                    .defaultFailureEvent()
                    .build();

    private static final AbstractFlowConfiguration.FlowEdgeConfig<MigrateZookeeperToKraftRollbackState, MigrateZookeeperToKraftRollbackStateSelectors>
            EDGE_CONFIG = new AbstractFlowConfiguration.FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE,
                    HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT);

    public MigrateZookeeperToKraftRollbackFlowConfig() {
        super(MigrateZookeeperToKraftRollbackState.class, MigrateZookeeperToKraftRollbackStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftRollbackState, MigrateZookeeperToKraftRollbackStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftRollbackState, MigrateZookeeperToKraftRollbackStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftRollbackStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftRollbackStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftRollbackStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftRollbackStateSelectors[]{
                START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Finalize Zookeeper to KRaft migration";
    }

    @Override
    public MigrateZookeeperToKraftRollbackStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
    }
}