package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_BROKER_NODES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_RESTART_KAFKA_CONNECT_NODES_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftRestartKafkaBrokerNodesHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRestartKafkaBrokerNodesHandler.class);

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftEvent> event) {
        LOGGER.error("Migrate Zookeeper to KRaft (restart Kafka broker nodes) failed.", e);
        return new MigrateZookeeperToKraftFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterModificationService clusterModificationService = getClusterModificationService(stackDto);
        try {
            if (clusterModificationService.isRolePresent(stackDto.getCluster().getName(), KAFKA_BROKER_ROLE, KAFKA_SERVICE_TYPE)) {
                clusterModificationService.restartServiceRoleByType(KAFKA_SERVICE_TYPE, KAFKA_BROKER_ROLE);
            } else {
                LOGGER.debug("{} role is not present in cluster. Skipping the restart step.", KAFKA_BROKER_ROLE);
            }
        } catch (Exception e) {
            LOGGER.error("Migrate Zookeeper to KRaft (restart Kafka broker nodes) failed.", e);
            return new MigrateZookeeperToKraftFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftEvent(START_RESTART_KAFKA_CONNECT_NODES_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return RESTART_KAFKA_BROKER_NODES_EVENT.selector();
    }

    private ClusterModificationService getClusterModificationService(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto).clusterModificationService();
    }
}
