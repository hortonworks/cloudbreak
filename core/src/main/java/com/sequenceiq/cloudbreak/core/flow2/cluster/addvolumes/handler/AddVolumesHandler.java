package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
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
public class AddVolumesHandler extends ExceptionCatcherEventHandler<AddVolumesHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private AddVolumesService addVolumesService;

    @Inject
    private ResourceService resourceService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AddVolumesHandlerEvent> addVolumesHandlerEvent) {
        LOGGER.debug("Starting to add additional volumes on DiskUpdateService");
        AddVolumesHandlerEvent payload = addVolumesHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        try {
            Stack stack = stackService.getById(stackId);
            Set<Resource> resources = new HashSet<>(resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup,
                    List.of(stack.getDiskResourceType())));
            VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, null, payload.getSize().intValue(),
                    payload.getType(), payload.getCloudVolumeUsageType());
            List<Resource> updatedResources = addVolumesService.createVolumes(resources, volumeRequest, payload.getNumberOfDisks().intValue(),
                    payload.getInstanceGroup(), stackId);
            resourceService.saveAll(updatedResources);
            LOGGER.info("Successfully created and saved volumes from request {} to all instances", payload);
            return new AddVolumesFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                    payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
        } catch (Exception e) {
            LOGGER.warn("Failed to add disks", e);
            return new AddVolumesFailedEvent(stackId, e);
        }
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}