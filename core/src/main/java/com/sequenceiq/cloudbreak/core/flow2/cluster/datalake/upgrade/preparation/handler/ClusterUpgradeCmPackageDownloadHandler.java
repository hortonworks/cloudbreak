package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_CM_PACKAGES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.upgrade.preparation.ClusterUpgradeCmPackageDownloaderService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeCmPackageDownloadHandler extends ExceptionCatcherEventHandler<ClusterUpgradePreparationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeCmPackageDownloadHandler.class);

    @Inject
    private ClusterUpgradeCmPackageDownloaderService clusterUpgradeCmPackageDownloaderService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradePreparationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade CM package download event {}", event);
        ClusterUpgradePreparationEvent request = event.getData();
        Long stackId = request.getResourceId();
        try {
            clusterUpgradeCmPackageDownloaderService.downloadCmPackages(stackId, request.getImageId());
            return new ClusterUpgradePreparationEvent(START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT.name(), stackId, request.getClouderaManagerProducts(),
                    request.getImageId());
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade CM package download failed.", e);
            return new ClusterUpgradePreparationFailureEvent(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return DOWNLOAD_CM_PACKAGES_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradePreparationEvent> event) {
        LOGGER.error("Cluster upgrade CM package download failed.", e);
        return new ClusterUpgradePreparationFailureEvent(resourceId, e);
    }
}
