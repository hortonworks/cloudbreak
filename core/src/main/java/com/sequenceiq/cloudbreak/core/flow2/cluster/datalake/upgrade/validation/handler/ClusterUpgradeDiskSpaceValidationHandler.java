package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.DiskSpaceValidationService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeDiskSpaceValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeDiskSpaceValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeDiskSpaceValidationHandler.class);

    @Inject
    private DiskSpaceValidationService diskSpaceValidationService;

    @Inject
    private StackService stackService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeDiskSpaceValidationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade validation event.");
        ClusterUpgradeDiskSpaceValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        try {
            diskSpaceValidationService.validateFreeSpaceForUpgrade(getStack(stackId), request.getRequiredFreeSpace());
            return new ClusterUpgradeDiskSpaceValidationFinishedEvent(request.getResourceId());
        } catch (UpgradeValidationFailedException e) {
            LOGGER.warn("Cluster upgrade validation failed", e);
            return new ClusterUpgradeValidationFailureEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade validation was unsuccessful due to an internal error", e);
            return new ClusterUpgradeDiskSpaceValidationFinishedEvent(request.getResourceId());
        }
    }

    private Stack getStack(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }

    @Override
    public String selector() {
        return VALIDATE_DISK_SPACE_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeDiskSpaceValidationEvent> event) {
        LOGGER.error("Cluster upgrade validation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradeDiskSpaceValidationFinishedEvent(resourceId);
    }
}
