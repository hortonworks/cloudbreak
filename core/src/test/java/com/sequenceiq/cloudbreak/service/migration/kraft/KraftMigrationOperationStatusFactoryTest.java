package com.sequenceiq.cloudbreak.service.migration.kraft;

import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.NOT_APPLICABLE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.core.flow2.chain.MigrateZookeeperToKraftFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftConfigurationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftFinalizationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftRollbackFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@ExtendWith(MockitoExtension.class)
class KraftMigrationOperationStatusFactoryTest {

    private static final long STACK_ID = 1L;

    @Mock
    private FlowLogDBService flowLogDBService;

    @Mock
    private FlowChainLogService flowChainLogService;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private KraftMigrationOperationStatusFactory underTest;

    @ParameterizedTest
    @EnumSource(KraftMigrationStatus.class)
    void testGetStatusFromClusterKRaftMigrationStatus(KraftMigrationStatus kraftMigrationStatus) {
        KraftMigrationOperationStatus result = underTest.getStatusFromClusterKRaftMigrationStatus(kraftMigrationStatus);

        KraftMigrationOperationStatus expected = switch (kraftMigrationStatus) {
            case ZOOKEEPER_INSTALLED -> ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;
            case PRE_MIGRATION, BROKERS_IN_MIGRATION -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
            case KRAFT_INSTALLED -> FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            case BROKERS_IN_KRAFT -> ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            default -> NOT_APPLICABLE;
        };
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{0} with finalized={1} and failed={2} should result in {3}")
    @MethodSource("flowLogStatusParameters")
    void testGetStatusFromFlowInformationFromFlowLogs(Class<?> flowConfigClass, boolean finalized, boolean failed,
            KraftMigrationOperationStatus expectedStatus) {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowId("flowId");
        flowLog.setFlowType(ClassValue.of(flowConfigClass));
        flowLog.setFinalized(finalized);
        flowLog.setCurrentState(failed ? "FAILED_STATE" : "FINISHED_STATE");
        flowLog.setStateStatus(failed ? StateStatus.FAILED : StateStatus.SUCCESSFUL);
        flowLog.setCreated(100L);

        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList())).thenReturn(List.of(flowLog));

        Optional<KraftMigrationOperationStatus> result = underTest.getStatusFromFlowInformation(stackDto);

        verify(flowLogDBService).findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList());
        verify(flowLogDBService, times(0)).getLatestFlowLogsByCrnInFlowChain(anyString());
        assertEquals(expectedStatus, result.orElse(NOT_APPLICABLE));
    }

    @ParameterizedTest(name = "Last log {0} but previous log failed should result in {1}")
    @MethodSource("previousLogFailedParameters")
    void testGetStatusFromFlowInformationWhenPreviousLogFailed(Class<?> flowConfigClass, KraftMigrationOperationStatus expectedStatus) {
        when(stackDto.getId()).thenReturn(STACK_ID);

        FlowLog lastLog = new FlowLog();
        lastLog.setFlowId("flowId");
        lastLog.setFlowType(ClassValue.of(flowConfigClass));
        lastLog.setFinalized(false);
        lastLog.setCurrentState("RUNNING_STATE");
        lastLog.setCreated(200L);

        FlowLog previousLog = new FlowLog();
        previousLog.setFlowId("flowId");
        previousLog.setFlowType(ClassValue.of(flowConfigClass));
        previousLog.setFinalized(false);
        previousLog.setCurrentState("FAILED_STATE");
        previousLog.setCreated(100L);

        FlowLog failedLog = new FlowLog();
        failedLog.setFlowId("flowId");
        failedLog.setFlowType(ClassValue.of(flowConfigClass));
        failedLog.setFinalized(false);
        failedLog.setStateStatus(StateStatus.FAILED);
        failedLog.setCurrentState("FAILED_STATE");
        failedLog.setCreated(90L);

        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList())).thenReturn(List.of(lastLog, previousLog, failedLog));

        Optional<KraftMigrationOperationStatus> result = underTest.getStatusFromFlowInformation(stackDto);

        assertEquals(expectedStatus, result.orElse(NOT_APPLICABLE));
    }

    @Test
    void testMigrationOperationStatusValuesShouldBeInDetailedStackStatusExceptExtremalStatuses() {
        Set<KraftMigrationOperationStatus> nonStackStatusRelevantStatues = Set.of(NOT_APPLICABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE);
        Arrays.stream(KraftMigrationOperationStatus.values())
                .filter(status -> !nonStackStatusRelevantStatues.contains(status))
                .forEach(status ->
                        assertEquals(status.name(), DetailedStackStatus.valueOf(status.name()).name()));
    }

    @ParameterizedTest(name = "{0} with finalized={1} and failed={2} should result in {3}")
    @MethodSource("flowChainLogWithFlowLogAndStatusParameters")
    void testGetStatusFromFlowChainInformationAndFlowLogs(Class<?> flowConfigClass, boolean finalized, boolean failed,
            KraftMigrationOperationStatus expectedStatus) {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog flowLog = new FlowLog();
        flowLog.setFlowType(ClassValue.of(flowConfigClass));
        flowLog.setFinalized(finalized);
        flowLog.setCurrentState(failed ? "FAILED_STATE" : "FINISHED_STATE");
        flowLog.setStateStatus(failed ? StateStatus.FAILED : StateStatus.SUCCESSFUL);
        flowLog.setCreated(100L);
        flowLog.setFlowChainId("flowChainId");
        mockMigrationFlowChain(List.of(flowLog));

        Optional<KraftMigrationOperationStatus> result = underTest.getStatusFromFlowInformation(stackDto);

        verify(flowLogDBService).findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList());
        assertEquals(expectedStatus, result.orElse(NOT_APPLICABLE));
    }

    @Test
    void testGetStatusFromFlowChainInformationAndFlowLogsWhenNoFlowRelatedFlowChain() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(STACK_ID,
                MigrateZookeeperToKraftFlowEventChainFactory.class.getSimpleName())).thenReturn(Optional.empty());

        Optional<KraftMigrationOperationStatus> result = underTest.getStatusFromFlowInformation(stackDto);

        verify(flowLogDBService).findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList());
        assertFalse(result.isPresent());
    }

    @Test
    void testGetStatusFromFlowChainInformationAndFlowLogsWhenMigrationTriggeredAfterRollback() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog rollbackFlowLog = new FlowLog();
        rollbackFlowLog.setFlowType(ClassValue.of(MigrateZookeeperToKraftRollbackFlowConfig.class));
        rollbackFlowLog.setFinalized(true);
        rollbackFlowLog.setCurrentState("FINISHED_STATE");
        rollbackFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        rollbackFlowLog.setCreated(90L);
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList())).thenReturn(List.of(rollbackFlowLog));

        FlowLog migrationFlowChainFlowLog = new FlowLog();
        migrationFlowChainFlowLog.setFlowType(ClassValue.of(MigrateZookeeperToKraftMigrationFlowConfig.class));
        migrationFlowChainFlowLog.setFinalized(true);
        migrationFlowChainFlowLog.setCurrentState("FINISHED_STATE");
        migrationFlowChainFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        migrationFlowChainFlowLog.setCreated(100L);
        migrationFlowChainFlowLog.setFlowChainId("flowChainId");
        mockMigrationFlowChain(List.of(migrationFlowChainFlowLog));

        Optional<KraftMigrationOperationStatus> result = underTest.getStatusFromFlowInformation(stackDto);

        verify(flowLogDBService).findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList());
        assertEquals(ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, result.orElse(NOT_APPLICABLE));
    }

    @ParameterizedTest
    @MethodSource("completedOperationParameters")
    void testNewerRollbackOrFinalizationTakesPrecedenceOverMigration(Class<?> flowConfigClass, KraftMigrationOperationStatus expectedStatus) {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog migrationLog = new FlowLog();
        migrationLog.setFlowType(ClassValue.of(MigrateZookeeperToKraftMigrationFlowConfig.class));
        migrationLog.setCreated(100L);
        migrationLog.setFinalized(true);
        mockMigrationFlowChain(List.of(migrationLog));

        FlowLog newerOperationLog = new FlowLog();
        newerOperationLog.setFlowType(ClassValue.of(flowConfigClass));
        newerOperationLog.setCreated(200L);
        newerOperationLog.setFinalized(true);
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList())).thenReturn(List.of(newerOperationLog));

        assertThat(underTest.getStatusFromFlowInformation(stackDto)).contains(expectedStatus);
    }

    @Test
    void testFailureInNestedUpscaleIsPreservedAfterFlowFinalization() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog failedLog = new FlowLog();
        failedLog.setFlowType(ClassValue.of(StackUpscaleConfig.class));
        failedLog.setFlowId("upscaleFlowId");
        failedLog.setCreated(100L);
        failedLog.setStateStatus(StateStatus.FAILED);

        FlowLog finishedLog = new FlowLog();
        finishedLog.setFlowType(ClassValue.of(StackUpscaleConfig.class));
        finishedLog.setFlowId("upscaleFlowId");
        finishedLog.setCreated(200L);
        finishedLog.setStateStatus(StateStatus.SUCCESSFUL);
        finishedLog.setFinalized(true);
        mockMigrationFlowChain(List.of(finishedLog, failedLog));

        assertThat(underTest.getStatusFromFlowInformation(stackDto)).contains(ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED);
    }

    @Test
    void testRollbackAfterFailedMigrationIsInProgress() {
        when(stackDto.getId()).thenReturn(STACK_ID);
        FlowLog failedMigrationLog = new FlowLog();
        failedMigrationLog.setFlowType(ClassValue.of(MigrateZookeeperToKraftMigrationFlowConfig.class));
        failedMigrationLog.setFlowId("migrationFlowId");
        failedMigrationLog.setCreated(100L);
        failedMigrationLog.setStateStatus(StateStatus.FAILED);
        mockMigrationFlowChain(List.of(failedMigrationLog));

        FlowLog rollbackLog = new FlowLog();
        rollbackLog.setFlowType(ClassValue.of(MigrateZookeeperToKraftRollbackFlowConfig.class));
        rollbackLog.setFlowId("rollbackFlowId");
        rollbackLog.setCreated(200L);
        rollbackLog.setFinalized(false);
        when(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(eq(STACK_ID), anyList())).thenReturn(List.of(rollbackLog));

        assertThat(underTest.getStatusFromFlowInformation(stackDto)).contains(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS);
    }

    private void mockMigrationFlowChain(List<FlowLog> logs) {
        FlowChainLog migrationChain = new FlowChainLog(MigrateZookeeperToKraftFlowEventChainFactory.class.getSimpleName(),
                "migrationChainId", null, "", "", "");
        FlowChainLog upscaleChain = new FlowChainLog("MigrateZookeeperToKraftFlowEventChainFactory/UpscaleFlowEventChainFactory",
                "upscaleChainId", "migrationChainId", "", "", "");
        when(flowChainLogService.findLastByResourceIdAndFlowChainTypeOrderByCreatedDesc(STACK_ID,
                MigrateZookeeperToKraftFlowEventChainFactory.class.getSimpleName())).thenReturn(Optional.of(migrationChain));
        when(flowChainLogService.collectRelatedFlowChains(migrationChain)).thenReturn(List.of(migrationChain, upscaleChain));
        Set<String> flowIds = Set.of("migrationFlowId", "upscaleFlowId");
        when(flowLogDBService.getFlowIdsByChainIds(Set.of("migrationChainId", "upscaleChainId"))).thenReturn(flowIds);
        when(flowLogDBService.getFlowLogsByFlowIdsCreatedDesc(flowIds, PageRequest.of(0, 3))).thenReturn(new PageImpl<>(logs));
    }

    private static Stream<Arguments> completedOperationParameters() {
        return Stream.of(
                Arguments.of(MigrateZookeeperToKraftRollbackFlowConfig.class, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE),
                Arguments.of(MigrateZookeeperToKraftFinalizationFlowConfig.class, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE));
    }

    private static Stream<Arguments> previousLogFailedParameters() {
        return Stream.of(
                Arguments.of(MigrateZookeeperToKraftConfigurationFlowConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftMigrationFlowConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftFinalizationFlowConfig.class, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftRollbackFlowConfig.class, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackSyncFlowConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackUpscaleConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(ClusterUpscaleFlowConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(ClusterDownscaleFlowConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackDownscaleConfig.class, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED)
        );
    }

    private static Stream<Arguments> flowLogStatusParameters() {
        return Stream.of(
                // Finalization Flow
                Arguments.of(MigrateZookeeperToKraftFinalizationFlowConfig.class, false, true, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftFinalizationFlowConfig.class, true, false, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE),
                Arguments.of(MigrateZookeeperToKraftFinalizationFlowConfig.class, false, false, FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),

                // Rollback Flow
                Arguments.of(MigrateZookeeperToKraftRollbackFlowConfig.class, false, true, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftRollbackFlowConfig.class, true, false, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE),
                Arguments.of(MigrateZookeeperToKraftRollbackFlowConfig.class, false, false, ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),

                // Edge cases
                Arguments.of(SetDefaultJavaVersionFlowConfig.class, true, false, NOT_APPLICABLE)
        );
    }

    private static Stream<Arguments> flowChainLogWithFlowLogAndStatusParameters() {
        return Stream.of(
                // Configuration Flow
                Arguments.of(MigrateZookeeperToKraftConfigurationFlowConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftConfigurationFlowConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(MigrateZookeeperToKraftConfigurationFlowConfig.class, false, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),

                // Upscale Related Flows
                Arguments.of(StackSyncFlowConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackSyncFlowConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(StackSyncFlowConfig.class, false, false, INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS),
                Arguments.of(StackUpscaleConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackUpscaleConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(StackUpscaleConfig.class, false, false, INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS),
                Arguments.of(ClusterUpscaleFlowConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(ClusterUpscaleFlowConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(ClusterUpscaleFlowConfig.class, false, false, INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS),
                Arguments.of(ClusterDownscaleFlowConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(ClusterDownscaleFlowConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(ClusterDownscaleFlowConfig.class, false, false, INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS),
                Arguments.of(StackDownscaleConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(StackDownscaleConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(StackDownscaleConfig.class, false, false, INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS),

                // Migration Flow
                Arguments.of(MigrateZookeeperToKraftMigrationFlowConfig.class, false, true, ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED),
                Arguments.of(MigrateZookeeperToKraftMigrationFlowConfig.class, true, false, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE),
                Arguments.of(MigrateZookeeperToKraftMigrationFlowConfig.class, false, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS)
        );
    }
}
