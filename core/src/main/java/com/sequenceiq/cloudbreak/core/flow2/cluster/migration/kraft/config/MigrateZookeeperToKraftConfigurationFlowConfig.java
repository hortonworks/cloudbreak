package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.FINAL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.INIT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.StackStatusFinalizerAbstractFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class MigrateZookeeperToKraftConfigurationFlowConfig extends StackStatusFinalizerAbstractFlowConfig<MigrateZookeeperToKraftConfigurationState,
        MigrateZookeeperToKraftConfigurationStateSelectors> implements RetryableFlowConfiguration<MigrateZookeeperToKraftConfigurationStateSelectors> {

    private static final List<Transition<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors>> TRANSITIONS =
            new Transition.Builder<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors>()
                    .defaultFailureEvent(MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT)

                    .from(INIT_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE)
                    .event(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE).to(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE)
                    .event(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT)
                    .defaultFailureEvent()

                    .from(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE).to(FINAL_STATE)
                    .event(FINALIZE_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE,
                    HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT);

    public MigrateZookeeperToKraftConfigurationFlowConfig() {
        super(MigrateZookeeperToKraftConfigurationState.class, MigrateZookeeperToKraftConfigurationStateSelectors.class);
    }

    @Override
    protected List<Transition<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<MigrateZookeeperToKraftConfigurationState, MigrateZookeeperToKraftConfigurationStateSelectors> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public MigrateZookeeperToKraftConfigurationStateSelectors[] getEvents() {
        return MigrateZookeeperToKraftConfigurationStateSelectors.values();
    }

    @Override
    public MigrateZookeeperToKraftConfigurationStateSelectors[] getInitEvents() {
        return new MigrateZookeeperToKraftConfigurationStateSelectors[]{
                START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT
        };
    }

    @Override
    public String getDisplayName() {
        return "Migrate Zookeeper to KRaft configuration";
    }

    @Override
    public MigrateZookeeperToKraftConfigurationStateSelectors getRetryableEvent() {
        return HANDLED_FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
    }

}

