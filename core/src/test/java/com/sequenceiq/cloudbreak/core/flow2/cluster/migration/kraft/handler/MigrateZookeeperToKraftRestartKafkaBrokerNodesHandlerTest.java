package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_CONNECT_NODES_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class MigrateZookeeperToKraftRestartKafkaBrokerNodesHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private MigrateZookeeperToKraftRestartKafkaBrokerNodesHandler underTest;

    @Test
    void testDoAcceptSuccess() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_BROKER_NODES_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftEvent.class, result);
        assertEquals(START_RESTART_KAFKA_CONNECT_NODES_EVENT.name(), result.getSelector());
        verify(clusterApi).restartKafkaBrokerNodes(stackDto);
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(RESTART_KAFKA_BROKER_NODES_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        doThrow(new CloudbreakException("error")).when(clusterApi).restartKafkaBrokerNodes(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), result.getSelector());
        verify(clusterApi).restartKafkaBrokerNodes(stackDto);
    }
}
