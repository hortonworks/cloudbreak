package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT;

import java.util.Collections;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftConfigurationValidationHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftConfigurationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftConfigurationValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private KraftMigrationService kraftMigrationService;

    @Inject
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftConfigurationEvent> event) {
        LOGGER.error("Migrate Zookeeper to KRaft configuration validation failed.", e);
        return new MigrateZookeeperToKraftFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            KraftMigrationStatusResponse kraftMigrationStatus = kraftMigrationService.getKraftMigrationStatus(stack, Collections.emptyList());
            zookeeperToKraftMigrationValidator.validateZookeeperToKraftMigrationState(kraftMigrationStatus.getKraftMigrationStatus());
        } catch (Exception e) {
            LOGGER.error("Migrate Zookeeper to KRaft configuration validation failed.", e);
            return new MigrateZookeeperToKraftConfigurationFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftConfigurationEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_INSTALLATION_EVENT.name(), event.getData().getResourceId(),
                event.getData().isKraftInstallNeeded());
    }

    @Override
    public String selector() {
        return MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_VALIDATION_EVENT.selector();
    }
}
