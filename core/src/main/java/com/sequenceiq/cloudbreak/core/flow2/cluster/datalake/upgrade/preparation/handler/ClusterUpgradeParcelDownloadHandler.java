package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_PARCELS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeParcelDownloadHandler extends ExceptionCatcherEventHandler<ClusterUpgradePreparationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeParcelDownloadHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradePreparationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade parcel download event {}", event);
        ClusterUpgradePreparationEvent request = event.getData();
        Long stackId = request.getResourceId();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            Set<ClouderaManagerProduct> clouderaManagerProducts = request.getClouderaManagerProducts();
            clusterApiConnectors.getConnector(stack).downloadParcels(clouderaManagerProducts);
            return new ClusterUpgradePreparationEvent(START_CLUSTER_UPGRADE_PARCEL_DISTRIBUTION_EVENT.name(), stackId, clouderaManagerProducts);
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade parcel download failed.", e);
            return new ClusterUpgradePreparationFailureEvent(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return DOWNLOAD_PARCELS_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradePreparationEvent> event) {
        LOGGER.error("Cluster upgrade parcel download failed.", e);
        return new ClusterUpgradePreparationFailureEvent(resourceId, e);
    }
}
