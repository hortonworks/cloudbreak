package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationHandlerSelectors.RESTART_KAFKA_ROLES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftMigrationStateSelectors.START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.ZookeeperToKraftKafkaRollingRestartRoleTypes.resolve;

import java.util.List;

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
public class MigrateZookeeperToKraftRestartKafkaRolesHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRestartKafkaRolesHandler.class);

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftEvent> event) {
        LOGGER.error("Migrate Zookeeper to KRaft (restart Kafka roles) failed.", e);
        return new MigrateZookeeperToKraftFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterModificationService clusterModificationService = getClusterModificationService(stackDto);
        boolean staleConfigsOnly = event.getData().isStaleConfigsOnly();
        boolean kraftHostGroupPresent = event.getData().isKraftHostGroupPresent();
        try {
            List<String> roleTypes = resolve(clusterModificationService, stackDto.getCluster().getName(), staleConfigsOnly, kraftHostGroupPresent);
            if (roleTypes.isEmpty()) {
                LOGGER.debug("No Kafka roles to restart before Zookeeper to KRaft migration.");
            } else {
                LOGGER.debug("Rolling restart for Kafka role types {} with staleConfigsOnly={}.", roleTypes, staleConfigsOnly);
                clusterModificationService.rollingRestartServiceRolesByType(KAFKA_SERVICE_TYPE, roleTypes, staleConfigsOnly);
            }
        } catch (Exception e) {
            LOGGER.error("Migrate Zookeeper to KRaft (restart Kafka roles) failed.", e);
            return new MigrateZookeeperToKraftFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftEvent(START_MIGRATE_ZOOKEEPER_TO_KRAFT_EVENT.name(), stackId, staleConfigsOnly, kraftHostGroupPresent);
    }

    @Override
    public String selector() {
        return RESTART_KAFKA_ROLES_EVENT.selector();
    }

    private ClusterModificationService getClusterModificationService(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto).clusterModificationService();
    }
}
