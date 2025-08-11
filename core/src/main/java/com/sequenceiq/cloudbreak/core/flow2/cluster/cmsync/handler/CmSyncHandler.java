package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.handler;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageUpdateService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationStatus;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.cloudbreak.service.upgrade.sync.template.ClusterManagerTemplateSyncService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CmSyncHandler extends ExceptionCatcherEventHandler<CmSyncRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncHandler.class);

    @Inject
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Inject
    private CmSyncerService cmSyncerService;

    @Inject
    private CmSyncImageUpdateService cmSyncImageUpdateService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterManagerTemplateSyncService clusterManagerTemplateSyncService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CmSyncRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CmSyncRequest> event) {
        LOGGER.debug("Reading CM and active parcel versions from CM server encountered an unexpected error ", e);
        String message = String.format("unexpected error: %s", e.getMessage());
        return new CmSyncResult(message, new CloudbreakServiceException(message, e), event.getData());
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CmSyncRequest> event) {
        CmSyncRequest request = event.getData();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            clusterManagerTemplateSyncService.sync(stack.getId());
            Set<Image> candidateImages = cmSyncImageCollectorService.collectImages(stack, request.getCandidateImageUuids());
            CmSyncOperationSummary cmSyncOperationSummary = cmSyncerService.syncFromCmToDb(stack, candidateImages);
            CmSyncOperationStatus cmSyncOperationStatus = cmSyncOperationSummary.getSyncOperationStatus();
            if (!cmSyncOperationStatus.hasSucceeded()) {
                LOGGER.debug("Reading CM and active parcel versions from CM server encountered failures. Details: {}", cmSyncOperationStatus.getMessage());
                Exception e = new CloudbreakServiceException(cmSyncOperationStatus.getMessage());
                return new CmSyncResult(cmSyncOperationStatus.getMessage(), e, request);
            }
            cmSyncImageUpdateService.updateImageAfterCmSync(stack, cmSyncOperationSummary, candidateImages);

            return new CmSyncResult(request, cmSyncOperationStatus.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Reading CM and active parcel versions from CM server resulted in error ", e);
            String message = String.format("unexpected error: %s", e.getMessage());
            return new CmSyncResult(message, new CloudbreakServiceException(message, e), request);
        }
    }

}
