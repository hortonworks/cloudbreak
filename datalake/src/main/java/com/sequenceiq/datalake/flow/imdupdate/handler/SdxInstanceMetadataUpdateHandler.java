package com.sequenceiq.datalake.flow.imdupdate.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.imdupdate.event.InstanceMetadataUpdateRequest;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateFailedEvent;
import com.sequenceiq.datalake.flow.imdupdate.event.SdxInstanceMetadataUpdateSuccessEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SdxInstanceMetadataUpdateHandler extends ExceptionCatcherEventHandler<InstanceMetadataUpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInstanceMetadataUpdateHandler.class);

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Inject
    private SdxService sdxService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(InstanceMetadataUpdateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<InstanceMetadataUpdateRequest> event) {
        LOGGER.warn("Exception during instance metadata update in SDX: ", e);
        return new SdxInstanceMetadataUpdateFailedEvent(resourceId, event.getData().getUserId(), e, "");
    }

    @Override
    protected Selectable doAccept(HandlerEvent<InstanceMetadataUpdateRequest> event) {
        LOGGER.debug("Entering instance metadata update in SDX, event: {}", event);
        InstanceMetadataUpdateRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            SdxCluster sdxCluster = sdxService.getById(request.getResourceId());
            cloudbreakStackService.updateInstanceMetadata(sdxCluster, request.getUpdateType());
            LOGGER.debug("Instance metadata update was called successfully in core.");
            return new SdxInstanceMetadataUpdateSuccessEvent(sdxId, userId);
        } catch (Exception e) {
            LOGGER.warn("Calling instance metadata update failed in core: ", e);
            return new SdxInstanceMetadataUpdateFailedEvent(sdxId, userId, e, e.getMessage());
        }
    }

}
