package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_FINISH_EVENT;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DistroXDiskUpdateHandler extends EventSenderAwareHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateHandler.class);

    @Inject
    private DiskUpdateService diskUpdateService;

    public DistroXDiskUpdateHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return DATAHUB_DISK_UPDATE_HANDLER_EVENT.selector();
    }

    @Override
    public void accept(Event<DistroXDiskUpdateEvent> datahubDiskUpdateEventEvent) {
        LOGGER.debug("In DistroXDiskUpdateHandler.accept");
        DistroXDiskUpdateEvent payload = datahubDiskUpdateEventEvent.getData();
        List<Volume> volumesToBeUpdated = payload.getVolumesToBeUpdated();
        Long stackId = payload.getStackId();
        DiskUpdateRequest diskUpdateRequest = payload.getDiskUpdateRequest();
        try {
            LOGGER.debug("Starting Disk Update for datahub.");
            LOGGER.debug("Calling updateDiskTypeAndSize with request :: {}", diskUpdateRequest);
            diskUpdateService.updateDiskTypeAndSize(diskUpdateRequest, volumesToBeUpdated, stackId);
            DistroXDiskUpdateEvent datahubDiskUpdateEvent = DistroXDiskUpdateEvent.builder()
                .withResourceCrn(payload.getResourceCrn())
                .withResourceId(payload.getResourceId())
                .withDiskUpdateRequest(payload.getDiskUpdateRequest())
                .withClusterName(payload.getClusterName())
                .withAccountId(payload.getAccountId())
                .withSelector(DATAHUB_DISK_UPDATE_FINISH_EVENT.selector())
                .withVolumesToBeUpdated(payload.getVolumesToBeUpdated())
                .withCloudPlatform(payload.getCloudPlatform())
                .withStackId(stackId)
                .build();
            eventSender().sendEvent(datahubDiskUpdateEvent, datahubDiskUpdateEventEvent.getHeaders());
        } catch (Exception ex) {
            DistroXDiskUpdateFailedEvent failedEvent =
                    new DistroXDiskUpdateFailedEvent(payload, ex, DATAHUB_DISK_UPDATE_FAILED);
            LOGGER.error("FAILED_DATAHUB_DISK_UPDATE_EVENT event sent with error: {} on stack {}", ex.getMessage(), payload.getResourceCrn());
            eventSender().sendEvent(failedEvent, datahubDiskUpdateEventEvent.getHeaders());
        }
    }
}
