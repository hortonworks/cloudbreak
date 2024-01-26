package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import java.util.HashSet;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesUnmountEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesUnmountFinishedEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteVolumesUnmountHandler extends ExceptionCatcherEventHandler<DeleteVolumesUnmountEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesUnmountHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Inject
    private ResourceService resourceService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesUnmountEvent> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesUnmountEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesUnmountEvent> deleteVolumesUnmountEvent) {
        LOGGER.debug("Staring DeleteVolumesUnmountHandler with event: {}", deleteVolumesUnmountEvent);
        DeleteVolumesUnmountEvent payload = deleteVolumesUnmountEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getRequestGroup();
        LOGGER.debug("Starting unmounting before deleting volumes to group {}", requestGroup);
        try {
            Stack stack = stackService.getByIdWithLists(stackId);
            List<Resource> resourceList = resourceService.findAllByStackIdAndResourceTypeIn(stackId,
                    List.of(stack.getDiskResourceType())).stream().filter(res -> null != res.getInstanceId()).toList();
            stack.setResources(new HashSet<>(resourceList));
            LOGGER.debug("Unmounting block storages before deleting block storage from the instance!");
            deleteVolumesService.unmountBlockStorageDisks(stack, requestGroup);
            return new DeleteVolumesUnmountFinishedEvent(stackId, requestGroup, payload.getResourcesToBeDeleted(),
                    payload.getStackDeleteVolumesRequest(), payload.getCloudPlatform(), payload.getHostTemplateServiceComponents());
        } catch (Exception ex) {
            LOGGER.warn("Unmounting disks after deleting block storage failed for stack: {}, and group: {}, Exception:: {}", stackId, requestGroup,
                    ex);
            return new DeleteVolumesFailedEvent(ex.getMessage(), ex, stackId);
        }
    }
}
