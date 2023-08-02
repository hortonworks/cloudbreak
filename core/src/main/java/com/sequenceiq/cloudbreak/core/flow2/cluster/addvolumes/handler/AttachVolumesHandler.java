package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AttachVolumesHandler extends ExceptionCatcherEventHandler<AttachVolumesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachVolumesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private AddVolumesService addVolumesService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AttachVolumesHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AttachVolumesHandlerEvent> attachVolumesHandlerEvent) {
        LOGGER.debug("Starting to add additional volumes on DiskUpdateService");
        AttachVolumesHandlerEvent payload = attachVolumesHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        try {
            Stack stack = stackService.getById(stackId);
            Set<Resource> resources = new HashSet<>(resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup,
                    List.of(stack.getDiskResourceType())));
            addVolumesService.attachVolumes(resources, stackId);
            LOGGER.info("Successfully attached volumes from request {} to all instances", payload);
            return new AttachVolumesFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                    payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
        } catch (Exception e) {
            LOGGER.warn("Failed to attach disks to the stack", e);
            return new AddVolumesFailedEvent(stackId, e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AttachVolumesHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}