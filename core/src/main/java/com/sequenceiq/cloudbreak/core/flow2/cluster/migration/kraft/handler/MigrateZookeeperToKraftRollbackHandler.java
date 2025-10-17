package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackHandlerSelectors.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftRollbackStateSelectors.FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftRollbackFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftRollbackHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftRollbackEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRollbackHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftRollbackEvent> event) {
        LOGGER.error("Rollback Zookeeper to KRaft migration failed.", e);
        return new MigrateZookeeperToKraftRollbackFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftRollbackEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = getClusterConnector(stackDto);
        try {
            connector.rollbackZookeeperToKraftMigration(stackDto);
        } catch (Exception e) {
            LOGGER.error("Rollback Zookeeper to KRaft migration failed.", e);
            return new MigrateZookeeperToKraftRollbackFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftRollbackEvent(FINISH_ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector();
    }

    private ClusterApi getClusterConnector(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto);
    }
}