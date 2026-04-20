package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationHandlerSelectors.MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftConfigurationStateSelectors.FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftConfigurationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftRemoveBrokerVersionHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftConfigurationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRemoveBrokerVersionHandler.class);

    private static final String KAFKA_BROKER_ROLE_TYPE = "KAFKA_BROKER";

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String INTER_BROKER_PROTOCOL_VERSION = "inter.broker.protocol.version";

    private final StackDtoService stackDtoService;

    private final ClusterApiConnectors clusterApiConnectors;

    public MigrateZookeeperToKraftRemoveBrokerVersionHandler(StackDtoService stackDtoService, ClusterApiConnectors clusterApiConnectors) {
        this.stackDtoService = stackDtoService;
        this.clusterApiConnectors = clusterApiConnectors;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftConfigurationEvent> event) {
        LOGGER.error("Removing broker version config during Zookeeper to KRaft migration failed.", e);
        return new MigrateZookeeperToKraftFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftConfigurationEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        String clusterName = stackDto.getCluster().getName();
        Optional<String> currentValue = connector.getRoleConfigValueByServiceType(
                clusterName, KAFKA_BROKER_ROLE_TYPE, KAFKA_SERVICE_TYPE, INTER_BROKER_PROTOCOL_VERSION);
        Optional<String> valueToClear = currentValue.filter(value -> !value.isBlank());
        if (valueToClear.isPresent()) {
            LOGGER.debug("Clearing {} (current value: [{}]) from {} role in {} service in cluster {}.",
                    INTER_BROKER_PROTOCOL_VERSION, valueToClear.get(), KAFKA_BROKER_ROLE_TYPE, KAFKA_SERVICE_TYPE, clusterName);
            try {
                connector.updateRoleConfigByServiceType(clusterName, KAFKA_BROKER_ROLE_TYPE, KAFKA_SERVICE_TYPE,
                        Collections.singletonMap(INTER_BROKER_PROTOCOL_VERSION, ""));
            } catch (Exception e) {
                LOGGER.error("Removing broker version config during Zookeeper to KRaft migration failed.", e);
                return new MigrateZookeeperToKraftConfigurationFailureEvent(stackId, e);
            }
        } else {
            LOGGER.info("{} is not set on {} role in {} service in cluster {}, skipping removal.",
                    INTER_BROKER_PROTOCOL_VERSION, KAFKA_BROKER_ROLE_TYPE, KAFKA_SERVICE_TYPE, clusterName);
        }
        return new MigrateZookeeperToKraftConfigurationEvent(FINISH_MIGRATE_ZOOKEEPER_TO_KRAFT_CONFIGURATION_EVENT.name(), stackId,
                event.getData().isKraftInstallNeeded());
    }

    @Override
    public String selector() {
        return MIGRATE_ZOOKEEPER_TO_KRAFT_REMOVE_BROKER_VERSION_EVENT.selector();
    }
}
