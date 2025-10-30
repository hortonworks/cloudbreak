package com.sequenceiq.cloudbreak.service.migration.kraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterKraftMigrationStatusService;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationAction;
import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowLogResponse;

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

    @InjectMocks
    private KraftMigrationService underTest;

    @ParameterizedTest
    @MethodSource("testGetKraftMigrationStatusParameters")
    void testGetKraftMigrationStatus(KraftMigrationStatus kraftMigrationStatus,
            boolean kraftMigrationSupported,
            KraftMigrationAction recommendedAction,
            boolean kraftMigrationRequired) {
        StackDto stack = setupStack(STACK_ID);
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterKraftMigrationStatusService clusterKraftMigrationStatusService = mock(ClusterKraftMigrationStatusService.class);
        String olderFlowId = UUID.randomUUID().toString();
        FlowLogResponse olderflowLogResponse = new FlowLogResponse();
        olderflowLogResponse.setFlowId(olderFlowId);
        olderflowLogResponse.setCurrentState(MigrateZookeeperToKraftConfigurationState.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_STATE.name());
        olderflowLogResponse.setCreated(1234L);
        String newerFlowId = UUID.randomUUID().toString();
        FlowLogResponse newerflowLogResponse = new FlowLogResponse();
        newerflowLogResponse.setFlowId(newerFlowId);
        newerflowLogResponse.setCurrentState(MigrateZookeeperToKraftMigrationState.MIGRATE_ZOOKEEPER_TO_KRAFT_STATE.name());
        newerflowLogResponse.setCreated(1235L);
        if (kraftMigrationSupported) {
            when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
            when(clusterApi.clusterKraftMigrationStatusService()).thenReturn(clusterKraftMigrationStatusService);
            when(clusterKraftMigrationStatusService.getKraftMigrationStatus()).thenReturn(kraftMigrationStatus);
        }
        when(zookeeperToKraftMigrationValidator.isMigrationFromZookeeperToKraftSupported(stack, stack.getAccountId())).thenReturn(kraftMigrationSupported);

        KraftMigrationStatusResponse expectedResponse = new KraftMigrationStatusResponse(kraftMigrationStatus.name(), recommendedAction.name(),
                kraftMigrationRequired, newerFlowId);
        KraftMigrationStatusResponse actualResponse = underTest.getKraftMigrationStatus(stack, List.of(olderflowLogResponse, newerflowLogResponse));
        assertEquals(expectedResponse.getKraftMigrationStatus(), actualResponse.getKraftMigrationStatus());
        assertEquals(expectedResponse.getRecommendedAction(), actualResponse.getRecommendedAction());
        assertEquals(expectedResponse.isKraftMigrationRequired(), actualResponse.isKraftMigrationRequired());
        assertEquals(expectedResponse.getFlowIdentifier(), actualResponse.getFlowIdentifier());
    }

    private static Stream<Arguments> testGetKraftMigrationStatusParameters() {
        return Stream.of(
                Arguments.of(KraftMigrationStatus.NOT_APPLICABLE, false, KraftMigrationAction.NO_ACTION, false),
                Arguments.of(KraftMigrationStatus.ZOOKEEPER_INSTALLED, true, KraftMigrationAction.MIGRATE, true),
                Arguments.of(KraftMigrationStatus.PRE_MIGRATION, true, KraftMigrationAction.NO_ACTION, false),
                Arguments.of(KraftMigrationStatus.BROKERS_IN_MIGRATION, true, KraftMigrationAction.NO_ACTION, false),
                Arguments.of(KraftMigrationStatus.BROKERS_IN_KRAFT, true, KraftMigrationAction.FINALIZE, false),
                Arguments.of(KraftMigrationStatus.KRAFT_INSTALLED, true, KraftMigrationAction.NO_ACTION, false)
        );
    }

    private StackDto setupStack(long stackId) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(stackId);
        lenient().when(stackDto.getStatus()).thenReturn(Status.AVAILABLE);
        Stack stack = new Stack();
        stack.setId(stackId);
        Cluster cluster = new Cluster();
        cluster.setId(2L);
        cluster.setName(CLUSTER_NAME);
        lenient().when(stackDto.getStack()).thenReturn(stack);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        lenient().when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        return stackDto;
    }
}