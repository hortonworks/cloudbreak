package com.sequenceiq.datalake.flow.repair.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairCouldNotStartEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairInProgressEvent;
import com.sequenceiq.datalake.flow.repair.event.SdxRepairStartRequest;
import com.sequenceiq.datalake.service.sdx.SdxRepairService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class SdxRepairStartHandler extends ExceptionCatcherEventHandler<SdxRepairStartRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairStartHandler.class);

    @Inject
    private SdxRepairService repairService;

    @Override
    public String selector() {
        return "SdxRepairStartRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxRepairStartRequest> event) {
        return new SdxRepairCouldNotStartEvent(resourceId, null, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SdxRepairStartRequest> event) {
        SdxRepairStartRequest request = event.getData();
        Long sdxId = request.getResourceId();
        String userId = request.getUserId();
        try {
            repairService.startSdxRepair(sdxId, request.getRepairSettings());
        } catch (Exception e) {
            LOGGER.error("Sdx repair start failed, sdxId: {}, error: {}", sdxId, e.getMessage());
            return new SdxRepairCouldNotStartEvent(sdxId, userId, e);
        }
        return new SdxRepairInProgressEvent(sdxId, userId);
    }
}
