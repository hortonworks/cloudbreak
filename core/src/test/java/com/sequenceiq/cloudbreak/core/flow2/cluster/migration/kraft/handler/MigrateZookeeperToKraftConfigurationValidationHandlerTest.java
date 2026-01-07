package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;
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
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class MigrateZookeeperToKraftConfigurationValidationHandlerTest {

    private static final boolean NO_KRAFT_INSTALL_NEEDED = false;

    private static final boolean KRAFT_INSTALL_NEEDED = true;

    private static final long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private KraftMigrationService kraftMigrationService;

    @Mock
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

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
        KraftMigrationStatusResponse response = new KraftMigrationStatusResponse(KraftMigrationStatus.ZOOKEEPER_INSTALLED.name(), "MIGRATE",
                true, null);
        when(kraftMigrationService.getKraftMigrationStatus(stackDto, Collections.emptyList())).thenReturn(response);
        doNothing().when(zookeeperToKraftMigrationValidator).validateZookeeperToKraftMigrationState(response.getKraftMigrationStatus());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationEvent.class, result);
        assertEquals(START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT.name(), result.getSelector());
    }

    @Test
    void testDoAcceptFailure() {
        StackDto stackDto = new StackDto();
        MigrateZookeeperToKraftConfigurationEvent request =
                new MigrateZookeeperToKraftConfigurationEvent(MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector(), STACK_ID,
                        NO_KRAFT_INSTALL_NEEDED);
        HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event = new HandlerEvent<>(new Event<>(request));
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        KraftMigrationStatusResponse response = new KraftMigrationStatusResponse(KraftMigrationStatus.BROKERS_IN_KRAFT.name(), "ROLLBACK",
                false, null);
        when(kraftMigrationService.getKraftMigrationStatus(stackDto, Collections.emptyList())).thenReturn(response);
        doThrow(new BadRequestException("error")).when(zookeeperToKraftMigrationValidator)
                .validateZookeeperToKraftMigrationState(response.getKraftMigrationStatus());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(MigrateZookeeperToKraftConfigurationFailureEvent.class, result);
        assertEquals(FAILED_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), result.getSelector());
    }
}