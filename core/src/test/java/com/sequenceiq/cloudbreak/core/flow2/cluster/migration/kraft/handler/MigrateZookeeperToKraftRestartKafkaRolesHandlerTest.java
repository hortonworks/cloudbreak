package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_ROLES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
public class MigrateZookeeperToKraftRestartKafkaRolesHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_CONNECT_ROLE = "KAFKA_CONNECT";

    private static final List<String> KAFKA_ROLE_TYPES = List.of("KRAFT", KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE);

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
    private MigrateZookeeperToKraftRestartKafkaRolesHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_ROLES_EVENT.selector(), STACK_ID, false, false);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterModificationService).rollingRestartServiceRolesByType(KAFKA_SERVICE_TYPE,
                List.of(KAFKA_BROKER_ROLE, KAFKA_CONNECT_ROLE), false);
    }

    @Test
    void testDoAcceptSuccessWhenStaleConfigsOnlyRestartNeeded() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_ROLES_EVENT.selector(), STACK_ID, true, true);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of("KRAFT", KAFKA_BROKER_ROLE));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterModificationService).rollingRestartServiceRolesByType(KAFKA_SERVICE_TYPE,
                List.of("KRAFT", KAFKA_BROKER_ROLE), true);
    }

    @Test
    void testDoAcceptSuccessWhenNoRolesPresent() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_ROLES_EVENT.selector(), STACK_ID, false, false);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterModificationService, never()).rollingRestartServiceRolesByType(any(), any(), anyBoolean());
    }

    @Test
    void testDoAcceptFailure() {
        String clusterName = "testCluster";
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_ROLES_EVENT.selector(), STACK_ID, false, false);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(clusterName);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterModificationService.getActiveServiceRoleTypes(clusterName, KAFKA_SERVICE_TYPE, KAFKA_ROLE_TYPES))
                .thenReturn(List.of(KAFKA_BROKER_ROLE));

        ArgumentCaptor<List<String>> roleTypesCaptor = ArgumentCaptor.forClass(List.class);
        doThrow(new RuntimeException("error")).when(clusterModificationService)
                .rollingRestartServiceRolesByType(eq(KAFKA_SERVICE_TYPE), roleTypesCaptor.capture(), eq(false));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        assertEquals(List.of(KAFKA_BROKER_ROLE), roleTypesCaptor.getValue());
    }
}
