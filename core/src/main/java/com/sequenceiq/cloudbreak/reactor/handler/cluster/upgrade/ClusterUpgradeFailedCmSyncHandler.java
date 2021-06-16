package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailHandledRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedCmSyncRequest;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class ClusterUpgradeFailedCmSyncHandler extends ExceptionCatcherEventHandler<ClusterUpgradeFailedCmSyncRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeFailedCmSyncHandler.class);

    @Inject
    private CmSyncerService cmSyncerService;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpgradeFailedCmSyncRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeFailedCmSyncRequest> event) {
        ClusterUpgradeFailedCmSyncRequest request = event.getData();
        return new ClusterUpgradeFailHandledRequest(resourceId, request.getException(), request.getDetailedStatus());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeFailedCmSyncRequest> event) {
        ClusterUpgradeFailedCmSyncRequest request = event.getData();
        try {
            Stack stack = stackService.getById(request.getResourceId());
            cmSyncerService.syncFromCmToDb(stack, request.getCandidateImages());
        } catch (Exception e) {
            LOGGER.warn("Error during syncing CM version to DB, syncing skipped.", e);
        }
        return new ClusterUpgradeFailHandledRequest(request.getResourceId(), request.getException(), request.getDetailedStatus());
    }
}
