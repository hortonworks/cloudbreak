package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftInstallHandlerTest {

    private static final long STACK_ID = 1L;

    private static final boolean KRAFT_INSTALL_NEEDED = true;

    private static final boolean NO_KRAFT_INSTALL_NEEDED = false;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private MigrateZookeeperToKraftInstallHandler underTest;

    @Test
    void testSelector() {
        assertEquals(MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.selector(), underTest.selector());
    }

    @Test
    void testDoAcceptSuccessWhenInstallNeeded() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.selector(), STACK_ID, KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(KRAFT_INSTALL_NEEDED, ((MigrateZookeeperToKraftConfigurationEvent) result).isKraftInstallNeeded());
        verify(clusterApi).installKraftAsStopped(stackDto);
    }

    @Test
    void testDoAcceptSuccessWhenInstallNotNeeded() {
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.selector(), STACK_ID, NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(NO_KRAFT_INSTALL_NEEDED, ((MigrateZookeeperToKraftConfigurationEvent) result).isKraftInstallNeeded());
        verifyNoInteractions(stackDtoService, clusterApiConnectors, clusterApi);
    }

    @Test
    void testDoAcceptFailureWhenInstallNeededAndClusterApiThrows() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.selector(), STACK_ID, KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("something")).when(clusterApi).installKraftAsStopped(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationFailureEvent.class, result);
        verify(clusterApi).installKraftAsStopped(stackDto);
    }

    @Test
    void testDefaultFailureEventCreatesFailureEvent() throws CloudbreakException {
        Long resourceId = STACK_ID;
        Exception exception = new RuntimeException("boom");
        MigrateZookeeperToKraftConfigurationEvent request = new MigrateZookeeperToKraftConfigurationEvent(
                MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALL_EVENT.selector(), STACK_ID, KRAFT_INSTALL_NEEDED);
        Event<MigrateZookeeperToKraftConfigurationEvent> event = new Event<>(request);

        Selectable result = underTest.defaultFailureEvent(resourceId, exception, event);

        assertInstanceOf(MigrateZookeeperToKraftFailureEvent.class, result);
        verify(stackDtoService, never()).getById(STACK_ID);
        verify(clusterApi, never()).installKraftAsStopped(new StackDto());
    }

}