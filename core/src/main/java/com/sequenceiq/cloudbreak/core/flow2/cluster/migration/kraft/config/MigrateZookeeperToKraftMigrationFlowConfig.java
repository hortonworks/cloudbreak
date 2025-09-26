package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.RESTART_KAFKA_BROKER_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState.RESTART_KAFKA_CONNECT_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_CONNECT_NODES_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class MigrateZookeeperToKraftMigrationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftMigrationState,
        MigrateZookeeperToKraftMigrationStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftMigrationStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors>()
                    .defaultFailureEvent(MigrateZookeeperToKraftMigrationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)

                    .from(INIT_STATE).to(RESTART_KAFKA_BROKER_NODES_STATE)
                    .event(START_RESTART_KAFKA_BROKER_NODES_EVENT)
                    .defaultFailureEvent()

                    .from(RESTART_KAFKA_BROKER_NODES_STATE).to(RESTART_KAFKA_CONNECT_NODES_STATE)
                    .event(START_RESTART_KAFKA_CONNECT_NODES_EVENT)
                    .defaultFailureEvent()

                    .from(RESTART_KAFKA_CONNECT_NODES_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE)
                    .event(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT);

    public MigrateZookeeperToKraftMigrationFlowConfig() {
        super(MigrateZookeeperToKraftMigrationState.class, MigrateZookeeperToKraftMigrationStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftMigrationState, MigrateZookeeperToKraftMigrationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftMigrationStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftMigrationStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftMigrationStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftMigrationStateSelectors[]{
                START_RESTART_KAFKA_BROKER_NODES_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate Zookeeper to KRaft";
    }

    @Override
    public MigrateZookeeperToKraftMigrationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
    }
}
