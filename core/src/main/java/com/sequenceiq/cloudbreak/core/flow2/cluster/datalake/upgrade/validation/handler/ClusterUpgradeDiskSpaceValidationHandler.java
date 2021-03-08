package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_CLUSTER_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.DiskSpaceValidationService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ClusterUpgradeDiskSpaceValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeDiskSpaceValidationHandler.class);

    @Inject
    private DiskSpaceValidationService diskSpaceValidationService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private StackService stackService;

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        LOGGER.debug("Accepting Cluster upgrade validation event.");
        ClusterUpgradeValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        Selectable result;
        try {
            StatedImage image = imageCatalogService.getImage(request.getImageId());
            diskSpaceValidationService.validateFreeSpaceForUpgrade(getStack(stackId), image.getImageCatalogUrl(), image.getImageCatalogName(),
                    request.getImageId());
            result = new ClusterUpgradeValidationFinishedEvent(stackId);
        } catch (UpgradeValidationFailedException e) {
            LOGGER.warn("Cluster upgrade validation failed", e);
            result = new ClusterUpgradeValidationFailureEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.warn("Cluster upgrade validation was unsuccessful due to an internal error", e);
            result = new ClusterUpgradeValidationFinishedEvent(stackId, e);
        }
        return result;
    }

    private Stack getStack(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }

    @Override
    public String selector() {
        return VALIDATE_CLUSTER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeValidationEvent> event) {
        return new ClusterUpgradeValidationFinishedEvent(resourceId, e);
    }
}
