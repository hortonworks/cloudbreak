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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterKraftMigrationStatusService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
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
        when(zookeeperToKraftMigrationValidator.isMigrationFromZookeeperToKraftSupported(stack, stack.getAccountId())).thenReturn(kraftMigrationSupported);
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
        when(zookeeperToKraftMigrationValidator.isMigrationFromZookeeperToKraftSupported(stack, stack.getAccountId())).thenReturn(kraftMigrationSupported);
        when(operationStatusFactory.getStatusFromFlowInformation(stack)).thenReturn(Optional.of(flowOperationStatus));

        KraftMigrationStatusResponse actualResponse = underTest.getKraftMigrationStatusResponse(stack);

        verifyNoInteractions(clusterApiConnectors);
        KraftMigrationStatusResponse expectedResponse = new KraftMigrationStatusResponse(flowOperationStatus.name(),
                recommendedAction.name(), kraftMigrationRequired);
        assertEquals(expectedResponse.getKraftMigrationStatus(), actualResponse.getKraftMigrationStatus());
        assertEquals(expectedResponse.getRecommendedAction(), actualResponse.getRecommendedAction());
        assertEquals(expectedResponse.isKraftMigrationRequired(), actualResponse.isKraftMigrationRequired());
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