package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftState.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class MigrateZookeeperToKraftFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftState,
        MigrateZookeeperToKraftStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftState, MigrateZookeeperToKraftStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftState, MigrateZookeeperToKraftStateSelectors>()
                    .defaultFailureEvent(MigrateZookeeperToKraftStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)

                    .from(INIT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE)
                    .event(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT)
                    .defaultFailureEvent()
                    .build();

    private static final FlowEdgeConfig<MigrateZookeeperToKraftState, MigrateZookeeperToKraftStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE, HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT);

    public MigrateZookeeperToKraftFlowConfig() {
        super(MigrateZookeeperToKraftState.class, MigrateZookeeperToKraftStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftState, MigrateZookeeperToKraftStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftState, MigrateZookeeperToKraftStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftStateSelectors[]{
                START_MIGRATE_ZOOKEEPER_TO_KRAFT_INIT_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate Zookeeper to KRaft";
    }

    @Override
    public MigrateZookeeperToKraftStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
    }
}
