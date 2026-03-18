package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_ROLLBACK_SKIPPED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationOperationStatusFactory;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftRollbackValidationHandlerTest {
    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private KraftMigrationService kraftMigrationService;

    @Mock
    private KraftMigrationOperationStatusFactory kraftMigrationOperationStatusFactory;

    @Mock
    private FlowMessageService flowMessageService;

    @InjectMocks
    private MigrateZookeeperToKraftRollbackValidationHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.BROKERS_IN_MIGRATION;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackEvent.class, result);
        assertEquals(START_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptSuccessWhenKraftMigrationRollbackAlreadyDone() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.ZOOKEEPER_INSTALLED;
        when(kraftMigrationOperationStatusFactory.getStatusFromFlowInformation(stackDto))
                .thenReturn(Optional.of(KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE));

        Selectable result = underTest.doAccept(event);

        String expectedMessage = "Skipping rollback Zookeeper to KRaft migration because rollback is executed recently or it is not in a rollbackable state: " +
                "[rollbackCompleted=true], [statusIsNotRollbackable=false]";
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_SKIPPED_EVENT, expectedMessage);
        assertInstanceOf(MigrateZookeeperToKraftRollbackEvent.class, result);
        assertEquals(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptSuccessWhenKraftMigrationAlreadyFinalized() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.KRAFT_INSTALLED;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        String expectedMessage = "Skipping rollback Zookeeper to KRaft migration because rollback is executed recently or it is not in a rollbackable state: " +
                "[rollbackCompleted=false], [statusIsNotRollbackable=true]";
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_ROLLBACK_SKIPPED_EVENT, expectedMessage);
        assertInstanceOf(MigrateZookeeperToKraftRollbackEvent.class, result);
        assertEquals(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptFailure() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftRollbackEvent request =
                new MigrateZookeeperToKraftRollbackEvent(ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector(), STACK_ID);
        HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        doThrow(new BadRequestException("error")).when(kraftMigrationService)
                .getKraftMigrationStatus(stackDto);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftRollbackFailureEvent.class, result);
        assertEquals(FAILED_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), result.getSelector());
    }
}