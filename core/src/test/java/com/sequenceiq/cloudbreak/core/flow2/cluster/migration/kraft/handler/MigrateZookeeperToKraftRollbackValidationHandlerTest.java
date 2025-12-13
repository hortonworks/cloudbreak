package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftRollbackValidationHandlerTest {
    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private KraftMigrationService kraftMigrationService;

    @Mock
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @InjectMocks
    private MigrateZookeeperToKraftRollbackValidationHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatusResponse response = new KraftMigrationStatusResponse(KraftMigrationStatus.BROKERS_IN_MIGRATION.name(), "ROLLBACK",
                false, null);
        when(kraftMigrationService.getKraftMigrationStatus(stackDto, Collections.emptyList())).thenReturn(response);
        doNothing().when(zookeeperToKraftMigrationValidator).validateZookeeperToKraftMigrationStateForRollback(response.getKraftMigrationStatus());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackEvent.class, result);
        assertEquals(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptFailure() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatusResponse response = new KraftMigrationStatusResponse(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name(), "ROLLBACK",
                true, null);
        when(kraftMigrationService.getKraftMigrationStatus(stackDto, Collections.emptyList())).thenReturn(response);
        doThrow(new BadRequestException("error")).when(zookeeperToKraftMigrationValidator)
                .validateZookeeperToKraftMigrationStateForRollback(response.getKraftMigrationStatus());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackFailureEvent.class, result);
        assertEquals(FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }
}