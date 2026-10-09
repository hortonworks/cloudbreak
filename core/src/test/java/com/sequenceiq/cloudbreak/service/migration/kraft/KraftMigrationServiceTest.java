package com.sequenceiq.cloudbreak.service.migration.kraft;

import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction.FINALIZE;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction.MIGRATE;
import static com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction.NO_ACTION;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.NOT_APPLICABLE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterKraftMigrationStatusService;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;

@ExtendWith(MockitoExtension.class)
class KraftMigrationServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CLUSTER_NAME = "test-cluster-name";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @Mock
    private KraftMigrationOperationStatusFactory operationStatusFactory;

    @InjectMocks
    private KraftMigrationService underTest;

    @ParameterizedTest
    @MethodSource("testGetKraftMigrationStatusResponseParameters")
    void testGetKraftMigrationStatusResponseWhenNoFlowLogs(KraftMigrationStatus kraftMigrationStatus,
            boolean kraftMigrationSupported,
            KraftMigrationAction recommendedAction,
            boolean kraftMigrationRequired,
            Status stackStatus,
            KraftMigrationOperationStatus expectedOperationStatus) {
        StackDto stack = setupStack(STACK_ID, stackStatus);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterKraftMigrationStatusService clusterKraftMigrationStatusService = mock(ClusterKraftMigrationStatusService.class);
        if (kraftMigrationSupported && stack.getStatus().isAvailable()) {
            when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
            when(clusterApi.clusterKraftMigrationStatusService()).thenReturn(clusterKraftMigrationStatusService);
            when(clusterKraftMigrationStatusService.getKraftMigrationStatus()).thenReturn(kraftMigrationStatus);
        }
        when(zookeeperToKraftMigrationValidator.isKraftMigrationStatusSupported(stack, stack.getAccountId())).thenReturn(kraftMigrationSupported);
        if (recommendedAction == MIGRATE) {
            when(zookeeperToKraftMigrationValidator.isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack)).thenReturn(true);
        }
        when(operationStatusFactory.getStatusFromFlowInformation(stack)).thenReturn(Optional.empty());
        if (!kraftMigrationSupported || !stack.getStatus().isAvailable()) {
            lenient().when(operationStatusFactory.getStatusFromClusterKRaftMigrationStatus(KraftMigrationStatus.NOT_APPLICABLE)).thenReturn(NOT_APPLICABLE);
        } else {
            when(operationStatusFactory.getStatusFromClusterKRaftMigrationStatus(kraftMigrationStatus)).thenReturn(expectedOperationStatus);
        }

        KraftMigrationStatusResponse actualResponse = underTest.getKraftMigrationStatusResponse(stack);

        if (kraftMigrationSupported && stack.getStatus().isAvailable()) {
            verify(clusterApiConnectors).getConnector(stack);
            verify(clusterApi).clusterKraftMigrationStatusService();
            verify(clusterKraftMigrationStatusService).getKraftMigrationStatus();
        } else {
            verifyNoInteractions(clusterApiConnectors, clusterApi, clusterKraftMigrationStatusService);
        }
        KraftMigrationStatusResponse expectedResponse = new KraftMigrationStatusResponse(expectedOperationStatus.name(),
                recommendedAction.name(), kraftMigrationRequired);
        assertEquals(expectedResponse.getKraftMigrationStatus(), actualResponse.getKraftMigrationStatus());
        assertEquals(expectedResponse.getRecommendedAction(), actualResponse.getRecommendedAction());
        assertEquals(expectedResponse.isKraftMigrationRequired(), actualResponse.isKraftMigrationRequired());
    }

    @ParameterizedTest
    @MethodSource("testGetKraftMigrationStatusResponseFromFlowLogParameters")
    void testGetKraftMigrationStatusResponseFromFlowLogs(KraftMigrationOperationStatus flowOperationStatus,
            boolean kraftMigrationSupported,
            KraftMigrationAction recommendedAction,
            boolean kraftMigrationRequired,
            Status stackStatus) {
        StackDto stack = setupStack(STACK_ID, stackStatus);
        when(zookeeperToKraftMigrationValidator.isKraftMigrationStatusSupported(stack, stack.getAccountId())).thenReturn(kraftMigrationSupported);
        if (recommendedAction == MIGRATE) {
            when(zookeeperToKraftMigrationValidator.isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack)).thenReturn(true);
        }
        when(operationStatusFactory.getStatusFromFlowInformation(stack)).thenReturn(Optional.of(flowOperationStatus));

        KraftMigrationStatusResponse actualResponse = underTest.getKraftMigrationStatusResponse(stack);

        verifyNoInteractions(clusterApiConnectors);
        KraftMigrationStatusResponse expectedResponse = new KraftMigrationStatusResponse(flowOperationStatus.name(),
                recommendedAction.name(), kraftMigrationRequired);
        assertEquals(expectedResponse.getKraftMigrationStatus(), actualResponse.getKraftMigrationStatus());
        assertEquals(expectedResponse.getRecommendedAction(), actualResponse.getRecommendedAction());
        assertEquals(expectedResponse.isKraftMigrationRequired(), actualResponse.isKraftMigrationRequired());
    }

    @Test
    void testGetKraftMigrationStatusForExistingMigrationWithoutNewRuntimeSupport() {
        StackDto stack = setupStack(STACK_ID, Status.AVAILABLE);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterKraftMigrationStatusService clusterKraftMigrationStatusService = mock(ClusterKraftMigrationStatusService.class);
        when(zookeeperToKraftMigrationValidator.isKraftMigrationStatusSupported(stack, stack.getAccountId())).thenReturn(true);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.clusterKraftMigrationStatusService()).thenReturn(clusterKraftMigrationStatusService);
        when(clusterKraftMigrationStatusService.getKraftMigrationStatus()).thenReturn(KraftMigrationStatus.BROKERS_IN_MIGRATION);

        assertEquals(KraftMigrationStatus.BROKERS_IN_MIGRATION, underTest.getKraftMigrationStatus(stack));
        verify(zookeeperToKraftMigrationValidator, never()).isZookeeperToKRaftMigrationSupportedForRuntimeVersion(stack);
    }

    @ParameterizedTest
    @MethodSource("runtimeVersionStatusResponseParameters")
    void testGetKraftMigrationStatusResponseWithActualRuntimeVersion(String cdhVersion, KraftMigrationStatus clusterMigrationStatus,
            boolean hasFlowLogs, KraftMigrationOperationStatus expectedStatus, KraftMigrationAction expectedAction) {
        StackDto stack = setupStack(STACK_ID, Status.AVAILABLE);
        when(stack.getStackVersion()).thenReturn("7.3.2");
        when(stack.getAccountId()).thenReturn("test-account");
        Blueprint blueprint = mock(Blueprint.class);
        when(stack.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getStackVersion()).thenReturn("7.2.17");
        when(blueprint.getBlueprintJsonText()).thenReturn("{}");
        ClusterComponentView cdhComponent = new ClusterComponentView();
        cdhComponent.setName("CDH");
        cdhComponent.setAttributes(new Json(new ClouderaManagerProduct().withName("CDH").withVersion(cdhVersion)));
        lenient().when(stack.getClusterComponents()).thenReturn(Set.of(cdhComponent));
        EntitlementService entitlementService = mock(EntitlementService.class);
        when(entitlementService.isZookeeperToKRaftMigrationEnabled("test-account")).thenReturn(true);
        BlueprintService blueprintService = mock(BlueprintService.class);
        when(blueprintService.anyOfTheServiceTypesPresentOnBlueprint("{}", List.of("KAFKA"))).thenReturn(true);
        ReflectionTestUtils.setField(underTest, "zookeeperToKraftMigrationValidator",
                new ZookeeperToKraftMigrationValidator(entitlementService, blueprintService, new ClouderaManagerProductsProvider()));
        KraftMigrationOperationStatus operationStatus = new KraftMigrationOperationStatusFactory()
                .getStatusFromClusterKRaftMigrationStatus(clusterMigrationStatus);
        when(operationStatusFactory.getStatusFromFlowInformation(stack)).thenReturn(hasFlowLogs ? Optional.of(operationStatus) : Optional.empty());
        if (!hasFlowLogs) {
            ClusterApi clusterApi = mock(ClusterApi.class);
            ClusterKraftMigrationStatusService statusService = mock(ClusterKraftMigrationStatusService.class);
            when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
            when(clusterApi.clusterKraftMigrationStatusService()).thenReturn(statusService);
            when(statusService.getKraftMigrationStatus()).thenReturn(clusterMigrationStatus);
            when(operationStatusFactory.getStatusFromClusterKRaftMigrationStatus(clusterMigrationStatus)).thenReturn(operationStatus);
        }

        KraftMigrationStatusResponse response = underTest.getKraftMigrationStatusResponse(stack);

        assertEquals(expectedStatus.name(), response.getKraftMigrationStatus());
        assertEquals(expectedAction.name(), response.getRecommendedAction());
        assertEquals(expectedAction == MIGRATE, response.isKraftMigrationRequired());
        verify(entitlementService).isZookeeperToKRaftMigrationEnabled("test-account");
        verify(blueprintService).anyOfTheServiceTypesPresentOnBlueprint("{}", List.of("KAFKA"));
    }

    private static Stream<Arguments> runtimeVersionStatusResponseParameters() {
        String olderRuntime = "7.3.2-1.cdh7.3.2.p100.80393083";
        String minimumRuntime = "7.3.2-1.cdh7.3.2.p10000.80393083";
        return Stream.of(
                Arguments.of(olderRuntime, KraftMigrationStatus.ZOOKEEPER_INSTALLED, false, NOT_APPLICABLE, NO_ACTION),
                Arguments.of(olderRuntime, KraftMigrationStatus.ZOOKEEPER_INSTALLED, true, NOT_APPLICABLE, NO_ACTION),
                Arguments.of(minimumRuntime, KraftMigrationStatus.ZOOKEEPER_INSTALLED, false, ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE, MIGRATE),
                Arguments.of(minimumRuntime, KraftMigrationStatus.ZOOKEEPER_INSTALLED, true, ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE, MIGRATE),
                Arguments.of(olderRuntime, KraftMigrationStatus.BROKERS_IN_KRAFT, false, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, FINALIZE),
                Arguments.of(olderRuntime, KraftMigrationStatus.BROKERS_IN_KRAFT, true, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, FINALIZE),
                Arguments.of(olderRuntime, KraftMigrationStatus.BROKERS_IN_MIGRATION, false, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS, NO_ACTION),
                Arguments.of(olderRuntime, KraftMigrationStatus.BROKERS_IN_MIGRATION, true, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS, NO_ACTION)
        );
    }

    private static Stream<Arguments> testGetKraftMigrationStatusResponseFromFlowLogParameters() {
        return Stream.of(
                Arguments.of(ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE, true, MIGRATE, true, Status.AVAILABLE),
                Arguments.of(ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, true, FINALIZE, false, Status.AVAILABLE),
                Arguments.of(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, true, NO_ACTION, false, Status.AVAILABLE),
                Arguments.of(ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE, false, NO_ACTION, false, Status.AVAILABLE)
        );
    }

    private static Stream<Arguments> testGetKraftMigrationStatusResponseParameters() {
        return Stream.of(
                Arguments.of(KraftMigrationStatus.NOT_APPLICABLE, false, NO_ACTION, false, Status.AVAILABLE, NOT_APPLICABLE),
                Arguments.of(KraftMigrationStatus.NOT_APPLICABLE, true, NO_ACTION, false, Status.UPDATE_IN_PROGRESS, NOT_APPLICABLE),
                Arguments.of(KraftMigrationStatus.ZOOKEEPER_INSTALLED, true, MIGRATE, true, Status.AVAILABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE),
                Arguments.of(KraftMigrationStatus.NOT_APPLICABLE, true, NO_ACTION, false, Status.UPDATE_IN_PROGRESS, NOT_APPLICABLE),
                Arguments.of(KraftMigrationStatus.NOT_APPLICABLE, true, NO_ACTION, false, Status.UPDATE_FAILED, NOT_APPLICABLE),
                Arguments.of(KraftMigrationStatus.PRE_MIGRATION, true, NO_ACTION, false, Status.AVAILABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(KraftMigrationStatus.BROKERS_IN_MIGRATION, true, NO_ACTION, false, Status.AVAILABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS),
                Arguments.of(KraftMigrationStatus.BROKERS_IN_KRAFT, true, FINALIZE, false, Status.AVAILABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE),
                Arguments.of(KraftMigrationStatus.KRAFT_INSTALLED, true, FINALIZE, false, Status.AVAILABLE, ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE)
        );
    }

    private StackDto setupStack(long stackId, Status stackStatus) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(stackId);
        lenient().when(stackDto.getStatus()).thenReturn(stackStatus);
        Stack stack = new Stack();
        stack.setId(stackId);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName(CLUSTER_NAME);
        when(stackDto.getResourceCrn()).thenReturn(TestUtil.STACK_CRN);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        return stackDto;
    }
}
