package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class AddVolumesOrchestrationHandler extends ExceptionCatcherEventHandler<AddVolumesOrchestrationHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesOrchestrationHandler.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private StackService stackService;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Inject
    private AddVolumesService addVolumesService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesOrchestrationHandlerEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<AddVolumesOrchestrationHandlerEvent> addVolumesOrchestrationHandlerEvent) {
        AddVolumesOrchestrationHandlerEvent payload = addVolumesOrchestrationHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getInstanceGroup();
        Selectable response;
        LOGGER.debug("Starting orchestration after adding volumes to group {}", payload.getInstanceGroup());
        try {
            Stack stack = stackService.getByIdWithLists(stackId);

            LOGGER.debug("Starting the mounting of additional volumes for group: {}", requestGroup);

            ResourceType diskResourceType = stack.getDiskResourceType();
            if (diskResourceType != null && diskResourceType.toString().contains("VOLUMESET")) {
                LOGGER.debug("Collecting resources based on stack id {} and resource type {} filtered by instance group {}.", stackId, diskResourceType,
                        requestGroup);
                List<Resource> resourceList = resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, requestGroup,
                        List.of(diskResourceType));
                stack.setResources(new HashSet<>(resourceList));
                Map<String, Map<String, String>> fstabInformation = addVolumesService.redeployStatesAndMountDisks(stack, requestGroup);

                diskUpdateService.parseFstabAndPersistDiskInformation(fstabInformation, stack);
                LOGGER.info("Successfully mounted additional volumes for group: {}", requestGroup);
                response = new AddVolumesOrchestrationFinishedEvent(stackId, payload.getNumberOfDisks(), payload.getType(), payload.getSize(),
                        payload.getCloudVolumeUsageType(), requestGroup);
            } else {
                LOGGER.warn("Failed to mount disks - No disks to mount");
                response = new AddVolumesFailedEvent(stackId, new NotFoundException("No disk found to mount!"));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to add disks", e);
            response = new AddVolumesFailedEvent(stackId, e);
        }
        return response;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<AddVolumesOrchestrationHandlerEvent> event) {
        return new AddVolumesFailedEvent(resourceId, e);
    }
}