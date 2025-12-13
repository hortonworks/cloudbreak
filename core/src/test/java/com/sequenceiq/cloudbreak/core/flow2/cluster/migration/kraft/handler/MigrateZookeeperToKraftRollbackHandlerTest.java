package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftRollbackHandlerTest {
    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private MigrateZookeeperToKraftRollbackHandler underTest;

    @Test
    void testDoAcceptSuccess() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request = new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector(),
                STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackEvent.class, result);
        assertEquals(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
        verify(clusterApi).rollbackZookeeperToKraftMigration(stackDto);
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request = new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector(),
                STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        doThrow(new CloudbreakException("error")).when(clusterApi).rollbackZookeeperToKraftMigration(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackFailureEvent.class, result);
        assertEquals(FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
        verify(clusterApi).rollbackZookeeperToKraftMigration(stackDto);
    }
}