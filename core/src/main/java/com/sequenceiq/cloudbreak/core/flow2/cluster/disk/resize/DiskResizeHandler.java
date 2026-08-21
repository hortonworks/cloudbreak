package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FAILURE_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DiskResizeHandler extends ExceptionCatcherEventHandler<DiskResizeHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskResizeHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DiskResizeHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DiskResizeHandlerRequest> event) {
        return new DiskResizeFailedEvent(FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DiskResizeHandlerRequest> diskResizeHandlerRequestEvent) {
        LOGGER.debug("Starting resizeDisks on DiskUpdateService");
        DiskResizeHandlerRequest payload = diskResizeHandlerRequestEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            ResourceType diskResourceType = stack.getDiskResourceType();
            if (ResourceType.isVolumeSet(diskResourceType)) {
                LOGGER.debug("Collecting resources based on stack id {} and resource type {} filtered by instance group {}.", stackId, diskResourceType,
                        instanceGroup);
                InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
                try {
                    diskUpdateService.resizeDisks(stack, instanceGroup);
                } finally {
                    InMemoryStateStore.deleteStack(stackId);
                }
                return new DiskResizeFinishedEvent(stackId);
            } else {
                LOGGER.warn("Failed to resize disks - No disks to resize");
                return new DiskResizeFailedEvent(FAILURE_EVENT.event(), stackId, new NotFoundException("No disk found to resize!"));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resize disks", e);
            return new DiskResizeFailedEvent(FAILURE_EVENT.event(), stackId, e);
        }
    }
}