package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftValidationHandlerTest {
    private static final long STACK_ID = 1L;

    @InjectMocks
    private MigrateZookeeperToKraftValidationHandler underTest;

    @Test
    void testDoAcceptShouldReturnStartMigrateZookeeperToKraftUpscaleKraftNodesEvent() {
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.name(), STACK_ID);
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_UPSCALE_KRAFT_NODES_EVENT.selector(), result.selector());
    }
}