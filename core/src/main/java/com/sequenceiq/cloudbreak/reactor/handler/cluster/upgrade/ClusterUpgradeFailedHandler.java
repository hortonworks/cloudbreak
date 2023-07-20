package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeFailedHandler extends ExceptionCatcherEventHandler<ClusterUpgradeFailedRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeFailedHandler.class);

    @Value("${cb.upgrade.failure.sync.sdx.enabled}")
    private boolean syncAfterFailureEnabled;

    @Inject
    private CmSyncerService cmSyncerService;

    @Inject
    private StackService stackService;

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Inject
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeFailedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeFailedRequest> event) {
        ClusterUpgradeFailedRequest request = event.getData();
        return new ClusterUpgradeFailHandledRequest(resourceId, request.getException(), request.getDetailedStatus());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeFailedRequest> event) {
        ClusterUpgradeFailedRequest request = event.getData();
        LOGGER.info("Handle ClusterUpgradeFailedRequest: {}", request);
        if (syncAfterFailureEnabled) {
            syncFromCmToCb(request);
        }
        runConclusionChecking(request);
        return new ClusterUpgradeFailHandledRequest(request.getResourceId(), request.getException(), request.getDetailedStatus());
    }

    private void syncFromCmToCb(ClusterUpgradeFailedRequest request) {
        try {
            LOGGER.debug("Starting syncing parcel and CM version from CM to DB.");
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            Set<Image> candidateImages = cmSyncImageCollectorService.collectImages(stack, Collections.emptySet());
            cmSyncerService.syncFromCmToDb(stack, candidateImages);
        } catch (Exception e) {
            LOGGER.warn("Error during syncing CM version to DB, syncing skipped.", e);
        }
    }

    private void runConclusionChecking(ClusterUpgradeFailedRequest request) {
        ResourceEvent resourceEvent = DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED == request.getDetailedStatus()
                ? CLUSTER_MANAGER_UPGRADE_FAILED : CLUSTER_UPGRADE_FAILED;
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), UPDATE_FAILED.name(), resourceEvent, ConclusionCheckerType.DEFAULT);
    }
}
