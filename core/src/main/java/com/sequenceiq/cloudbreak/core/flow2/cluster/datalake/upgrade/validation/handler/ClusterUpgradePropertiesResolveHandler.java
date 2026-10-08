package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradePropertiesResolveHandler extends ExceptionCatcherEventHandler<ClusterUpgradeValidationTriggerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePropertiesResolveHandler.class);

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Override
    public String selector() {
        return RESOLVE_CLUSTER_UPGRADE_PROPERTIES_EVENT.event();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeValidationTriggerEvent> event) {
        ClusterUpgradeValidationTriggerEvent request = event.getData();
        LOGGER.info("Resolving cluster upgrade properties for target image {}", request.getImageId());
        ClusterUpgradeProperties properties = clusterUpgradePropertiesResolver.resolve(request);
        return new ClusterUpgradeValidationEvent(CLUSTER_UPGRADE_PROPERTIES_RESOLVED_EVENT.event(), request.getResourceId(),
                properties.targetImage().imageId(), properties);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeValidationTriggerEvent> event) {
        LOGGER.error("Failed to resolve cluster upgrade properties", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
