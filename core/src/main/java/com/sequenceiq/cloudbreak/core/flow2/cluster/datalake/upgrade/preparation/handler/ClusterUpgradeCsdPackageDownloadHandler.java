package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.DOWNLOAD_CSD_PACKAGES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClouderaManagerCsdDownloaderService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeCsdPackageDownloadHandler extends ExceptionCatcherEventHandler<ClusterUpgradePreparationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeCsdPackageDownloadHandler.class);

    @Inject
    private ClouderaManagerCsdDownloaderService clouderaManagerCsdDownloaderService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradePreparationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade CSD package download event {}", event);
        ClusterUpgradePreparationEvent request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        try {
            clouderaManagerCsdDownloaderService.downloadCsdFiles(stackDto, true, request.getClouderaManagerProducts(), true);
            return new ClusterUpgradePreparationEvent(FINISH_CLUSTER_UPGRADE_PREPARATION_EVENT.name(), stackId, request.getClouderaManagerProducts(),
                    request.getImageId());
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade CSD package download failed.", e);
            return new ClusterUpgradePreparationFailureEvent(stackId, e);
        }
    }

    @Override
    public String selector() {
        return DOWNLOAD_CSD_PACKAGES_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradePreparationEvent> event) {
        LOGGER.error("Cluster upgrade CSD package download failed.", e);
        return new ClusterUpgradePreparationFailureEvent(resourceId, e);
    }
}
