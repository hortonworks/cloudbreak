package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINISH_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DistroXDiskUpdateHandler extends ExceptionCatcherEventHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateHandler.class);

    @Inject
    private DiskUpdateService diskUpdateService;

    @Override
    public String selector() {
        return DATAHUB_DISK_UPDATE_HANDLER_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DistroXDiskUpdateEvent> event) {
        return new DistroXDiskUpdateFailedEvent(event.getData(), e, DATAHUB_DISK_UPDATE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DistroXDiskUpdateEvent> datahubDiskUpdateEventEvent) {
        DistroXDiskUpdateEvent payload = datahubDiskUpdateEventEvent.getData();
        List<Volume> volumesToBeUpdated = payload.getVolumesToBeUpdated();
        Long stackId = payload.getStackId();
        DiskUpdateRequest diskUpdateRequest = payload.getDiskUpdateRequest();
        try {
            LOGGER.debug("Starting Disk Update for datahub. Calling updateDiskTypeAndSize with request :: {}", diskUpdateRequest);
            diskUpdateService.updateDiskTypeAndSize(diskUpdateRequest, volumesToBeUpdated, stackId);
            return DistroXDiskUpdateEvent.builder()
                .withResourceId(payload.getResourceId())
                .withDiskUpdateRequest(payload.getDiskUpdateRequest())
                .withClusterName(payload.getClusterName())
                .withAccountId(payload.getAccountId())
                .withSelector(DATAHUB_DISK_UPDATE_FINISH_EVENT.selector())
                .withVolumesToBeUpdated(payload.getVolumesToBeUpdated())
                .withCloudPlatform(payload.getCloudPlatform())
                .withStackId(stackId)
                .build();
        } catch (Exception ex) {
            LOGGER.warn("FAILED_DATAHUB_DISK_UPDATE_EVENT event sent on stack {}", payload.getStackId(), ex);
            return new DistroXDiskUpdateFailedEvent(payload, ex, DATAHUB_DISK_UPDATE_FAILED);
        }
    }
}
