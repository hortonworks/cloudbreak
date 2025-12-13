package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftFinalizationHandlerTest {
    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterApi clusterApi;

    @InjectMocks
    private MigrateZookeeperToKraftFinalizationHandler underTest;

    @Test
    void testDoAcceptSuccess() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftFinalizationEvent request = new MigrateZookeeperToKraftFinalizationEvent(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector(),
                STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
        verify(clusterApi).finalizeZookeeperToKraftMigration(stackDto);
    }

    @Test
    void testDoAcceptFailure() throws Exception {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftFinalizationEvent request = new MigrateZookeeperToKraftFinalizationEvent(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector(),
                STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        doThrow(new CloudbreakException("error")).when(clusterApi).finalizeZookeeperToKraftMigration(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationFailureEvent.class, result);
        assertEquals(FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
        verify(clusterApi).finalizeZookeeperToKraftMigration(stackDto);
    }
}