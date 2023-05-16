package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskResizeFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateFailedEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DistroXDiskUpdateResizeHandler extends EventSenderAwareHandler<DistroXDiskUpdateEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateResizeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    protected DistroXDiskUpdateResizeHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return "DATAHUB_DISK_RESIZE_HANDLER_EVENT";
    }

    @Override
    public void accept(Event<DistroXDiskUpdateEvent> diskResizeHandlerRequestEvent) {
        LOGGER.debug("Starting resizeDisks on DiskUpdateService");
        DistroXDiskUpdateEvent payload = diskResizeHandlerRequestEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getDiskUpdateRequest().getGroup();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            ResourceType diskResourceType = stack.getDiskResourceType();
            if (diskResourceType != null && diskResourceType.toString().contains("VOLUMESET")) {
                diskUpdateService.resizeDisksAndUpdateFstab(stack, instanceGroup);
                eventSender().sendEvent(new DistroXDiskResizeFinishedEvent(stackId), diskResizeHandlerRequestEvent.getHeaders());
            } else {
                LOGGER.error("Failed to resize disks - No disks to resize");
                eventSender().sendEvent(new DistroXDiskUpdateFailedEvent(payload, new NotFoundException("No disk found to resize!"),
                        DATAHUB_DISK_UPDATE_RESIZE_FAILED), diskResizeHandlerRequestEvent.getHeaders());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to resize disks", e);
            eventSender().sendEvent(new DistroXDiskUpdateFailedEvent(payload, e, DATAHUB_DISK_UPDATE_RESIZE_FAILED),
                    diskResizeHandlerRequestEvent.getHeaders());
        }
    }
}
