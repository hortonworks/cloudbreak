package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftFinalizationHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftFinalizationEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftFinalizationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftFinalizationEvent> event) {
        LOGGER.error("Finalize Zookeeper to KRaft migration failed.", e);
        return new MigrateZookeeperToKraftFinalizationFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = getClusterConnector(stackDto);
        try {
            connector.finalizeZookeeperToKraftMigration(stackDto);
        } catch (Exception e) {
            LOGGER.error("Finalize Zookeeper to KRaft migration failed.", e);
            return new MigrateZookeeperToKraftFinalizationFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftFinalizationEvent(FINISH_FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_EVENT.selector();
    }

    private ClusterApi getClusterConnector(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto);
    }
}