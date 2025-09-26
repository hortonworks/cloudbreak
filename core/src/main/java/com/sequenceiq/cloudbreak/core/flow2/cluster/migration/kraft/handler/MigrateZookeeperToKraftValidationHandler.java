package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftValidationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftValidationFailureEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftValidationHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftValidationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftValidationHandler.class);

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftValidationEvent> event) {
        LOGGER.error("Migrate Zookeeper to KRaft validation failed.", e);
        return new MigrateZookeeperToKraftValidationFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftValidationEvent> event) {
        Long stackId = event.getData().getResourceId();
        return new MigrateZookeeperToKraftValidationEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return MIGRATE_ZOOKEEPER_TO_KRAFT_VALIDATION_EVENT.selector();
    }
}
