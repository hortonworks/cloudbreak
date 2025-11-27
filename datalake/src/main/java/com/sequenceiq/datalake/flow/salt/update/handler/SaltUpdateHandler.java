package com.sequenceiq.datalake.flow.salt.update.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateFailureResponse;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateRequest;
import com.sequenceiq.datalake.flow.salt.update.event.SaltUpdateSuccessResponse;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SaltUpdateHandler extends ExceptionCatcherEventHandler<SaltUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltUpdateHandler.class);

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SaltUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SaltUpdateRequest> event) {
        LOGGER.warn("Exception during initiating Salt update: ", e);
        return new SaltUpdateFailureResponse(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SaltUpdateRequest> event) {
        LOGGER.debug("Initiating Salt update, event: {}", event);
        SaltUpdateRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        boolean skipHighstate = request.isSkipHighstate();
        try {
            SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
            cloudbreakStackService.updateSaltByName(sdxCluster, skipHighstate);
            LOGGER.debug("Salt update was successfully initiated in core.");
            return new SaltUpdateSuccessResponse(sdxId, userId);
        } catch (Exception e) {
            LOGGER.warn("Initiating Salt update failed in core: ", e);
            return new SaltUpdateFailureResponse(sdxId, userId, e);
        }
    }

}
