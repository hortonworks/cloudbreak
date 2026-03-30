package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_KRAFT_MIGRATION_SKIPPED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftConfigurationValidationHandlerTest {

    private static final boolean NO_KRAFT_INSTALL_NEEDED = false;

    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private KraftMigrationService kraftMigrationService;

    @Mock
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @Mock
    private FlowMessageService flowMessageService;

    @InjectMocks
    private MigrateZookeeperToKraftConfigurationValidationHandler underTest;

    @Test
    void testDoAcceptSuccess() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request =
                new MigrateZookeeperToKraftConfigurationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), STACK_ID,
                        NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.ZOOKEEPER_INSTALLED;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);
        doNothing().when(zookeeperToKraftMigrationValidator).validateZookeeperToKraftMigrationState(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptSuccessWhenKraftMigrationAlreadyDone() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request =
                new MigrateZookeeperToKraftConfigurationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), STACK_ID,
                        NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.BROKERS_IN_KRAFT;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        String expectedMessage = "Skipping Zookeeper to KRaft migration because cluster is already migrated to KRaft";
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_SKIPPED_EVENT, expectedMessage);
        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptSuccessWhenKraftMigrationFinalizationAlreadyDone() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request =
                new MigrateZookeeperToKraftConfigurationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), STACK_ID,
                        NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.KRAFT_INSTALLED;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        String expectedMessage = "Skipping Zookeeper to KRaft migration because cluster is already migrated to KRaft";
        verify(flowMessageService).fireEventAndLog(STACK_ID, AVAILABLE.name(), CLUSTER_KRAFT_MIGRATION_SKIPPED_EVENT, expectedMessage);
        assertEquals(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptFailure() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request =
                new MigrateZookeeperToKraftConfigurationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), STACK_ID,
                        NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatus kraftMigrationStatus = KraftMigrationStatus.ZOOKEEPER_INSTALLED;
        when(kraftMigrationService.getKraftMigrationStatus(stackDto)).thenReturn(kraftMigrationStatus);
        doThrow(new BadRequestException("error")).when(zookeeperToKraftMigrationValidator)
                .validateZookeeperToKraftMigrationState(kraftMigrationStatus);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
    }
}