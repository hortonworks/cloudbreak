package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
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
class MigrateZookeeperToKraftHandlerTest {
    private static final long STACK_ID = 1L;

    @InjectMocks
    private MigrateZookeeperToKraftHandler underTest;

    @Test
    void testDoAcceptShouldReturnFinishMigrateZookeeperToKraftEvent() {
        MigrateZookeeperToKraftEvent request = new MigrateZookeeperToKraftEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), STACK_ID);
        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.selector(), result.selector());
    }
}