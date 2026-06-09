package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.PARCEL_CLEANUP_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeParcelCleanupHandler extends ExceptionCatcherEventHandler<ClusterUpgradeValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeParcelCleanupHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ParcelService parcelService;

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeValidationEvent> event) {
        LOGGER.debug("Accepting cluster upgrade parcel cleanup event {}", event);
        ClusterUpgradeValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        try {
            ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolveUnchecked(request);
            StackDto stackDto = stackDtoService.getById(stackId);
            Set<ClouderaManagerProduct> clouderaManagerProducts = parcelService.getRequiredProductsFromProducts(stackDto,
                    clusterUpgradeProperties.getAllTargetProducts());
            parcelService.removeUnusedParcelVersions(stackDto, clouderaManagerProducts);
            return new ClusterUpgradeValidationEvent(START_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.event(), stackId,
                    clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties);
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade parcel cleanup failed.", e);
            return new ClusterUpgradeValidationFailureEvent(stackId, e);
        }
    }

    @Override
    public String selector() {
        return PARCEL_CLEANUP_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeValidationEvent> event) {
        LOGGER.error("Cluster upgrade parcel cleanup failed.", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
