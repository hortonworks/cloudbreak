package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

import java.util.Collections;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.migration.kraft.KraftMigrationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.validation.ZookeeperToKraftMigrationValidator;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftFinalizationValidationHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftFinalizationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftFinalizationValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private KraftMigrationService kraftMigrationService;

    @Inject
    private ZookeeperToKraftMigrationValidator zookeeperToKraftMigrationValidator;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftFinalizationEvent> event) {
        LOGGER.error("Finalize Zookeeper to KRaft migration validation failed.", e);
        return new MigrateZookeeperToKraftFinalizationFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stack = stackDtoService.getById(stackId);
        try {
            KraftMigrationStatusResponse kraftMigrationStatus = kraftMigrationService.getKraftMigrationStatus(stack, Collections.emptyList());
            zookeeperToKraftMigrationValidator.validateZookeeperToKraftMigrationStateForFinalization(kraftMigrationStatus.getKraftMigrationStatus());
        } catch (Exception e) {
            LOGGER.error("Finalize Zookeeper to KRaft migration validation failed.", e);
            return new MigrateZookeeperToKraftFinalizationFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftFinalizationEvent(START_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_VALIDATION_EVENT.selector();
    }
}