package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FAILURE_EVENT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
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
    private ResourceService resourceService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

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
            diskUpdateService.updateDiskTypeAndSize(
                    payload.getInstanceGroup(),
                    payload.getVolumeType(),
                    payload.getSize(),
                    payload.getVolumesToUpdate(),
                    stackId);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            ResourceType diskResourceType = stack.getDiskResourceType();
            if (diskResourceType != null && diskResourceType.toString().contains("VOLUMESET")) {
                LOGGER.debug("Collecting resources based on stack id {} and resource type {} filtered by instance group {}.", stackId, diskResourceType,
                        instanceGroup);
                List<Resource> resourceList = resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup,
                                List.of(diskResourceType)).stream().filter(res -> null != res.getInstanceId()).toList();
                stack.setResources(new HashSet<>(resourceList));
                Set<Node> allNodesInTargetGroup = stackUtil.collectNodes(stack).stream().filter(node -> node.getHostGroup().equals(instanceGroup))
                        .collect(Collectors.toSet());
                Cluster cluster = stack.getCluster();
                InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
                hostOrchestrator.resizeDisksOnNodes(
                        gatewayConfigService.getAllGatewayConfigs(stack),
                        allNodesInTargetGroup,
                        clusterDeletionBasedModel(stack.getId(), cluster.getId()));
                InMemoryStateStore.deleteStack(stackId);
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