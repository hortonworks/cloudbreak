package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationHandlerSelectors.REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.MigrateZookeeperToKraftFinalizationStateSelectors.FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.event.MigrateZookeeperToKraftFinalizationFailureEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class MigrateZookeeperToKraftRemoveUnusedZookeeperHandler extends ExceptionCatcherEventHandler<MigrateZookeeperToKraftFinalizationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateZookeeperToKraftRemoveUnusedZookeeperHandler.class);

    private static final String ZOOKEEPER_SERVICE_TYPE = "ZOOKEEPER";

    private static final String STREAMS_MESSAGING_BLUEPRINT_MARKER = "Streams Messaging";

    private static final String ZOOKEEPER_REMOVAL_STACK_VERSION = "7.3.2";

    private static final int ZOOKEEPER_REMOVAL_PATCH_VERSION = 10000;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MigrateZookeeperToKraftFinalizationEvent> event) {
        LOGGER.error("Remove unused ZooKeeper after KRaft migration finalization failed.", e);
        return new MigrateZookeeperToKraftFinalizationFailureEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MigrateZookeeperToKraftFinalizationEvent> event) {
        Long stackId = event.getData().getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        if (!shouldRemoveUnusedZookeeperAfterKraftFinalization(stackDto)) {
            LOGGER.debug("Skipping ZooKeeper service removal after KRaft finalization for stack {} "
                            + "(requires Streams Messaging blueprint on {} SP1 with patch {})",
                    stackId, ZOOKEEPER_REMOVAL_STACK_VERSION, ZOOKEEPER_REMOVAL_PATCH_VERSION);
            return new MigrateZookeeperToKraftFinalizationEvent(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), stackId);
        }
        ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
        try {
            connector.stopClouderaManagerService(ZOOKEEPER_SERVICE_TYPE, true);
            connector.deleteClouderaManagerService(ZOOKEEPER_SERVICE_TYPE);
        } catch (Exception e) {
            LOGGER.error("Remove unused ZooKeeper after KRaft migration finalization failed.", e);
            return new MigrateZookeeperToKraftFinalizationFailureEvent(stackId, e);
        }
        return new MigrateZookeeperToKraftFinalizationEvent(FINISH_REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.name(), stackId);
    }

    @Override
    public String selector() {
        return REMOVE_UNUSED_ZOOKEEPER_AFTER_KRAFT_FINALIZATION_EVENT.selector();
    }

    private boolean shouldRemoveUnusedZookeeperAfterKraftFinalization(StackDto stackDto) {
        return isStreamsMessagingBlueprint(stackDto.getBlueprint()) && isEligibleRuntimeVersion(stackDto);
    }

    private boolean isStreamsMessagingBlueprint(Blueprint blueprint) {
        if (blueprint == null || StringUtils.isBlank(blueprint.getName())) {
            return false;
        }
        return blueprint.getName().contains(STREAMS_MESSAGING_BLUEPRINT_MARKER);
    }

    private boolean isEligibleRuntimeVersion(StackDto stackDto) {
        if (stackDto.getCluster() == null) {
            return false;
        }
        return clusterComponentConfigProvider.getCdhProduct(stackDto.getCluster().getId())
                .map(ClouderaManagerProduct::getVersion)
                .filter(this::isZookeeperRemovalRuntimeVersion)
                .isPresent();
    }

    private boolean isZookeeperRemovalRuntimeVersion(String version) {
        return ZOOKEEPER_REMOVAL_STACK_VERSION.equals(
                CdhVersionProvider.getCdhStackVersionFromVersionString(version).orElse(null))
                && CdhVersionProvider.getCdhPatchVersionFromVersionString(version)
                        .filter(patch -> patch >= ZOOKEEPER_REMOVAL_PATCH_VERSION)
                        .isPresent();
    }
}
