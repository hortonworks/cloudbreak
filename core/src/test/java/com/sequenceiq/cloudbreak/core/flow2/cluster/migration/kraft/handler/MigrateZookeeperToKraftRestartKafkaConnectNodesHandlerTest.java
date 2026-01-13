package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_CONNECT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class MigrateZookeeperToKraftRestartKafkaConnectNodesHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_CONNECT_ROLE = "KAFKA_CONNECT";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private MigrateZookeeperToKraftRestartKafkaConnectNodesHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_CONNECT_NODES_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.isRolePresent(clusterName, KAFKA_CONNECT_ROLE, KAFKA_SERVICE_TYPE)).thenReturn(true);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterModificationService).restartServiceRoleByType(KAFKA_SERVICE_TYPE, KAFKA_CONNECT_ROLE);
    }

    @Test
    void testDoAcceptSuccessWhenConnectRoleNotFound() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_CONNECT_NODES_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.isRolePresent(clusterName, KAFKA_CONNECT_ROLE, KAFKA_SERVICE_TYPE)).thenReturn(false);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());

        verify(clusterModificationService, times(0)).restartServiceRoleByType(KAFKA_SERVICE_TYPE, KAFKA_CONNECT_ROLE);
    }

    @Test
    void testDoAcceptFailure() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_BROKER_NODES_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.isRolePresent(clusterName, KAFKA_CONNECT_ROLE, KAFKA_SERVICE_TYPE)).thenReturn(true);

        doThrow(new RuntimeException("error")).when(clusterModificationService).restartServiceRoleByType(KAFKA_SERVICE_TYPE, KAFKA_CONNECT_ROLE);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterModificationService).restartServiceRoleByType(KAFKA_SERVICE_TYPE, KAFKA_CONNECT_ROLE);
    }
}
