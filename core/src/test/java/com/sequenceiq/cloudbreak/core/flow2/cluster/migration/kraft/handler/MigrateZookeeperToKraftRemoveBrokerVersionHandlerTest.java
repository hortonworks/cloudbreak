package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftRemoveBrokerVersionHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String CLUSTER_NAME = "testCluster";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_SERVICE = "KAFKA";

    private static final String INTER_BROKER_PROTOCOL_VERSION = "inter.broker.protocol.version";

    private static final boolean KRAFT_INSTALL_NEEDED = true;

    private static final boolean NO_KRAFT_INSTALL_NEEDED = false;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Captor
    private ArgumentCaptor<Map<String, String>> brokerConfigCaptor;

    @InjectMocks
    private MigrateZookeeperToKraftRemoveBrokerVersionHandler underTest;

    @Test
    void testSelector() {
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector(), underTest.selector());
    }

    @Test
    void testDoAcceptClearsInterBrokerProtocolVersionWhenPresent() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector(), STACK_ID, KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(CLUSTER_NAME);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, INTER_BROKER_PROTOCOL_VERSION))
                .thenReturn(Optional.of("3.5"));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(KRAFT_INSTALL_NEEDED, ((MigrateZookeeperToKraftConfigurationEvent) result).isKraftInstallNeeded());

        verify(clusterApi).updateRoleConfigByServiceType(eq(CLUSTER_NAME), eq(KAFKA_BROKER_ROLE), eq(KAFKA_SERVICE), brokerConfigCaptor.capture());
        Map<String, String> config = brokerConfigCaptor.getValue();
        assertEquals(1, config.size());
        assertNull(config.get(INTER_BROKER_PROTOCOL_VERSION));
    }

    @Test
    void testDoAcceptSkipsUpdateWhenInterBrokerProtocolVersionNotSet() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector(), STACK_ID, NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(CLUSTER_NAME);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, INTER_BROKER_PROTOCOL_VERSION))
                .thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
        assertEquals(NO_KRAFT_INSTALL_NEEDED, ((MigrateZookeeperToKraftConfigurationEvent) result).isKraftInstallNeeded());
        verify(clusterApi, never()).updateRoleConfigByServiceType(
                eq(CLUSTER_NAME), eq(KAFKA_BROKER_ROLE), eq(KAFKA_SERVICE), anyMap());
    }

    @Test
    void testDoAcceptReturnsConfigurationFailureWhenUpdateThrows() throws Exception {
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector(), STACK_ID, NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getName()).thenReturn(CLUSTER_NAME);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        when(clusterApi.getRoleConfigValueByServiceType(CLUSTER_NAME, KAFKA_BROKER_ROLE, KAFKA_SERVICE, INTER_BROKER_PROTOCOL_VERSION))
                .thenReturn(Optional.of("3.5"));
        doThrow(new CloudbreakException("cm error")).when(clusterApi).updateRoleConfigByServiceType(
                eq(CLUSTER_NAME), eq(KAFKA_BROKER_ROLE), eq(KAFKA_SERVICE), anyMap());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDefaultFailureEventCreatesMigrateZookeeperToKraftFailureEvent() {
        Long resourceId = STACK_ID;
        Exception exception = new RuntimeException("boom");
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector(), STACK_ID, KRAFT_INSTALL_NEEDED);
        Event<MigrateZookeeperToKraftConfigurationEvent> event = new Event<>(request);

        Selectable result = underTest.defaultFailureEvent(resourceId, exception, event);

        assertInstanceOf(MigrateZookeeperToKraftFailureEvent.class, result);
        verify(stackDtoService, never()).getById(STACK_ID);
    }
}
