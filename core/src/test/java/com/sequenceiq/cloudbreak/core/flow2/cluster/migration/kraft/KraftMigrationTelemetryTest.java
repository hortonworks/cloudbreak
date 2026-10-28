package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_CONFIGURATION_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_CONFIGURATION_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_FINALIZATION_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_FINALIZATION_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_ROLLBACK_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.KRAFT_MIGRATION_ROLLBACK_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value;
import com.sequenceiq.cloudbreak.core.flow2.chain.MigrateZookeeperToKraftFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.chain.UpscaleFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftConfigurationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftFinalizationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftRollbackFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleState;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleState;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseMapper;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

class KraftMigrationTelemetryTest {

    private static final String MIGRATION_CHAIN = MigrateZookeeperToKraftFlowEventChainFactory.class.getSimpleName();

    private final ClusterUseCaseMapper mapper = new ClusterUseCaseMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapper, "flowConfigurations", List.of(
                new MigrateZookeeperToKraftConfigurationFlowConfig(), new MigrateZookeeperToKraftMigrationFlowConfig(),
                new MigrateZookeeperToKraftFinalizationFlowConfig(), new MigrateZookeeperToKraftRollbackFlowConfig(),
                new StackUpscaleConfig(), new ClusterUpscaleFlowConfig()));
        MigrateZookeeperToKraftFlowEventChainFactory chain = new MigrateZookeeperToKraftFlowEventChainFactory();
        ReflectionTestUtils.setField(chain, "configurationFlowConfig", new MigrateZookeeperToKraftConfigurationFlowConfig());
        ReflectionTestUtils.setField(chain, "migrationFlowConfig", new MigrateZookeeperToKraftMigrationFlowConfig());
        ReflectionTestUtils.setField(chain, "upscaleFlowEventChainFactory", new UpscaleFlowEventChainFactory());
        ReflectionTestUtils.setField(mapper, "flowEventChainFactories", List.of(chain));
        ReflectionTestUtils.invokeMethod(mapper, "init");
    }

    @ParameterizedTest
    @MethodSource("outcomes")
    void mapsKraftOutcome(AbstractFlowConfiguration<?, ?> config, Enum<? extends FlowState> state, Value expectedStatus) {
        FlowDetails flow = flowDetails(config, state, null);

        assertThat(mapper.useCase(flow)).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("chainedOutcomes")
    void mapsKraftOutcomeInsideMigrationChain(AbstractFlowConfiguration<?, ?> config, Enum<? extends FlowState> state, Value expectedStatus) {
        FlowDetails flow = flowDetails(config, state, MIGRATION_CHAIN);

        assertThat(mapper.useCase(flow)).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("nonOutcomeStates")
    void ignoresNonOutcomeStates(AbstractFlowConfiguration<?, ?> config, Enum<? extends FlowState> state) {
        assertThat(mapper.useCase(flowDetails(config, state, null))).isEqualTo(UNSET);
        if (state instanceof MigrateZookeeperToKraftConfigurationState || state instanceof MigrateZookeeperToKraftMigrationState) {
            assertThat(mapper.useCase(flowDetails(config, state, MIGRATION_CHAIN))).isEqualTo(UNSET);
        }
    }

    @ParameterizedTest
    @MethodSource("upscaleFailures")
    void mapsNestedUpscaleFailureToMigrationFailure(AbstractFlowConfiguration<?, ?> config, Enum<? extends FlowState> state) {
        FlowDetails flow = flowDetails(config, state, MIGRATION_CHAIN + "/UpscaleFlowEventChainFactory");

        assertThat(mapper.useCase(flow)).isEqualTo(KRAFT_MIGRATION_FAILED);
    }

    @Test
    void doesNotReportNestedUpscaleSuccessAsMigrationSuccess() {
        FlowDetails flow = flowDetails(new ClusterUpscaleFlowConfig(), ClusterUpscaleState.FINALIZE_UPSCALE_STATE,
                MIGRATION_CHAIN + "/UpscaleFlowEventChainFactory");

        assertThat(mapper.useCase(flow)).isEqualTo(UNSET);
    }

    private static FlowDetails flowDetails(AbstractFlowConfiguration<?, ?> config, Enum<? extends FlowState> state, String chain) {
        FlowDetails flow = new FlowDetails();
        flow.setFlowType(config.getClass().getSimpleName());
        flow.setNextFlowState(state.name());
        flow.setFlowChainType(chain);
        return flow;
    }

    private static Stream<Arguments> chainedOutcomes() {
        return outcomes().filter(arguments -> arguments.get()[1] instanceof MigrateZookeeperToKraftConfigurationState
                || arguments.get()[1] instanceof MigrateZookeeperToKraftMigrationState);
    }

    private static Stream<Arguments> upscaleFailures() {
        return Stream.of(
                Arguments.of(new StackUpscaleConfig(), StackUpscaleState.UPSCALE_FAILED_STATE),
                Arguments.of(new ClusterUpscaleFlowConfig(), ClusterUpscaleState.CLUSTER_UPSCALE_FAILED_STATE));
    }

    private static Stream<Arguments> nonOutcomeStates() {
        Set<Object> outcomes = outcomes().map(arguments -> arguments.get()[1]).collect(Collectors.toSet());
        List<AbstractFlowConfiguration<?, ?>> configs = List.of(
                new MigrateZookeeperToKraftConfigurationFlowConfig(), new MigrateZookeeperToKraftMigrationFlowConfig(),
                new MigrateZookeeperToKraftFinalizationFlowConfig(), new MigrateZookeeperToKraftRollbackFlowConfig());
        return configs.stream().flatMap(config -> Arrays.stream(config.getStateType().getEnumConstants())
                .filter(state -> !outcomes.contains(state)).map(state -> Arguments.of(config, state)));
    }

    private static Stream<Arguments> outcomes() {
        return Stream.of(
                Arguments.of(new MigrateZookeeperToKraftConfigurationFlowConfig(),
                        MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FINISHED_STATE,
                        KRAFT_MIGRATION_CONFIGURATION_FINISHED),
                Arguments.of(new MigrateZookeeperToKraftConfigurationFlowConfig(),
                        MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_FAILED_STATE,
                        KRAFT_MIGRATION_CONFIGURATION_FAILED),
                Arguments.of(new MigrateZookeeperToKraftMigrationFlowConfig(),
                        MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FINISHED_STATE,
                        KRAFT_MIGRATION_FINISHED),
                Arguments.of(new MigrateZookeeperToKraftMigrationFlowConfig(),
                        MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_FAILED_STATE,
                        KRAFT_MIGRATION_FAILED),
                Arguments.of(new MigrateZookeeperToKraftFinalizationFlowConfig(),
                        MigrateZookeeperToKraftFinalizationState.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE,
                        KRAFT_MIGRATION_FINALIZATION_FINISHED),
                Arguments.of(new MigrateZookeeperToKraftFinalizationFlowConfig(),
                        MigrateZookeeperToKraftFinalizationState.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE,
                        KRAFT_MIGRATION_FINALIZATION_FAILED),
                Arguments.of(new MigrateZookeeperToKraftRollbackFlowConfig(),
                        MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FINISHED_STATE,
                        KRAFT_MIGRATION_ROLLBACK_FINISHED),
                Arguments.of(new MigrateZookeeperToKraftRollbackFlowConfig(),
                        MigrateZookeeperToKraftRollbackState.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED_STATE,
                        KRAFT_MIGRATION_ROLLBACK_FAILED));
    }
}
