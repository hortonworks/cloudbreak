package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftFinalizationValidationHandlerTest {
    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private KraftMigrationService kraftMigrationService;

    @Mock
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @InjectMocks
    private MigrateZookeeperToKraftFinalizationValidationHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.BROKERS_IN_KRAFT;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);
        doNothing().when(zookeeperToKraftMigrationValidator).validateZookeeperToKraftMigrationStateForFinalization(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationEvent.class, result);
        assertEquals(START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptFailure() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftFinalizationEvent request =
                new MigrateZookeeperToKraftFinalizationEvent(FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.ZOOKEEPER_INSTALLED;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);
        doThrow(new BadRequestException("error")).when(zookeeperToKraftMigrationValidator)
                .validateZookeeperToKraftMigrationStateForFinalization(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftFinalizationFailureEvent.class, result);
        assertEquals(FAILED_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }
}