package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftValidationHandlerTest {
    private static final long STACK_ID = 1L;

    @InjectMocks
    private MigrateZookeeperToKraftValidationHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        MigrateZookeeperToKraftValidationEvent request = new MigrateZookeeperToKraftValidationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.selector(),
                STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftValidationEvent> event = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftValidationEvent.class, result);
        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.name(), result.getSelector());
    }
}