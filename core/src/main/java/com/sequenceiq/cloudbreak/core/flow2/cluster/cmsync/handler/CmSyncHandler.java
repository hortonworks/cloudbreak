package com.sequenceiq.cloudbreak.core.flow2.cluster.cmsync.handler;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CmSyncResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncImageCollectorService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.CmSyncerService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class CmSyncHandler extends ExceptionCatcherEventHandler<CmSyncRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmSyncHandler.class);

    @Inject
    private CmSyncImageCollectorService cmSyncImageCollectorService;

    @Inject
    private CmSyncerService cmSyncerService;

    @Inject
    private StackService stackService;

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
            Stack stack = stackService.getById(request.getResourceId());
            Set<Image> candidateImages = cmSyncImageCollectorService.collectImages(request.getFlowTriggerUserCrn(), stack, request.getCandidateImageUuids());
            CmSyncOperationSummary cmSyncOperationSummary = cmSyncerService.syncFromCmToDb(stack, candidateImages);
            if (!cmSyncOperationSummary.hasSucceeded()) {
                LOGGER.debug("Reading CM and active parcel versions from CM server encountered failures. Details: {}", cmSyncOperationSummary.getMessage());
                Exception e = new CloudbreakServiceException(cmSyncOperationSummary.getMessage());
                return new CmSyncResult(cmSyncOperationSummary.getMessage(), e, request);
            }
            return new CmSyncResult(request, cmSyncOperationSummary.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Reading CM and active parcel versions from CM server resulted in error ", e);
            String message = String.format("unexpected error: %s", e.getMessage());
            return new CmSyncResult(message, new CloudbreakServiceException(message, e), request);
        }
    }

}
