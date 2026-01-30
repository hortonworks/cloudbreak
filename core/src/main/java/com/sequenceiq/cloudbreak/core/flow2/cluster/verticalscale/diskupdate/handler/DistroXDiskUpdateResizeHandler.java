package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskResizeFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DistroXDiskUpdateResizeHandler extends ExceptionCatcherEventHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateResizeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Override
    public String selector() {
        return "DATAHUB_DISK_RESIZE_HANDLER_EVENT";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DistroXDiskUpdateEvent> event) {
        return new DistroXDiskUpdateFailedEvent(event.getData(), e, DATAHUB_DISK_UPDATE_RESIZE_FAILED);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DistroXDiskUpdateEvent> diskResizeHandlerRequestEvent) {
        DistroXDiskUpdateEvent payload = diskResizeHandlerRequestEvent.getData();
        LOGGER.debug("Starting resizeDisks on DiskUpdateService with request {}", payload);
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getGroup();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            ResourceType diskResourceType = stack.getDiskResourceType();
            if (diskResourceType != null && diskResourceType.toString().contains("VOLUMESET")) {
                diskUpdateService.resizeDisks(stack, instanceGroup);
                return new DistroXDiskResizeFinishedEvent(stackId);
            } else {
                LOGGER.warn("Failed to resize disks - No disks to resize");
                return new DistroXDiskUpdateFailedEvent(payload, new NotFoundException("No disk found to resize!"), DATAHUB_DISK_UPDATE_RESIZE_FAILED);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resize disks", e);
            return new DistroXDiskUpdateFailedEvent(payload, e, DATAHUB_DISK_UPDATE_RESIZE_FAILED);
        }
    }
}
