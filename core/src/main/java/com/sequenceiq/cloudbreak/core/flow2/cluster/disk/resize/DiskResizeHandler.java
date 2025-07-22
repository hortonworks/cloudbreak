package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FAILURE_EVENT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
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
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private DiskUpdateService diskUpdateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DiskResizeHandlerRequest.class);
    }

    private void parseFstabAndPersistDiskInformation(Map<String, Map<String, String>> fstabInformation, Stack stack) {
        LOGGER.debug("Parsing fstab information from host orchestrator resize disks - {}", fstabInformation);
        fstabInformation.forEach((hostname, value) -> {
            Optional<String> instanceIdOptional = stack.getInstanceMetaDataAsList().stream()
                .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                .map(InstanceMetaData::getInstanceId)
                .findFirst();

            if (instanceIdOptional.isPresent()) {
                String uuids = value.getOrDefault("uuids", "");
                String fstab = value.getOrDefault("fstab", "");
                if (!StringUtils.isEmpty(uuids) && !StringUtils.isEmpty(fstab)) {
                    LOGGER.debug("Persisting resources for instance id - {}, hostname - {}, uuids - {}, fstab - {}.", instanceIdOptional.get(), hostname,
                        uuids, fstab);
                    persistUuidAndFstab(stack, instanceIdOptional.get(), hostname, uuids, fstab);
                }
            }
        });
    }

    private void persistUuidAndFstab(Stack stack, String instanceId, String discoveryFQDN, String uuids, String fstab) {
        resourceService.saveAll(stack.getDiskResources().stream()
            .filter(volumeSet -> instanceId.equals(volumeSet.getInstanceId()))
            .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                volumeSetAttributes.setUuids(uuids);
                volumeSetAttributes.setFstab(fstab);
                if (!discoveryFQDN.equals(volumeSetAttributes.getDiscoveryFQDN())) {
                    LOGGER.info("DiscoveryFQDN is updated for {} to {}", volumeSet.getResourceName(), discoveryFQDN);
                }
                volumeSetAttributes.setDiscoveryFQDN(discoveryFQDN);
                resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
            }))
            .collect(Collectors.toList()));
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
                Set<Node> allNodes = stackUtil.collectNodes(stack);
                Cluster cluster = stack.getCluster();
                InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
                Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack);
                List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
                ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
                LOGGER.debug("Calling host orchestrator for resizing and fetching fstab information for nodes - {}", allNodes);
                Map<String, Map<String, String>> fstabInformation = hostOrchestrator.resizeDisksOnNodes(gatewayConfigs, nodesWithDiskData, allNodes,
                        exitCriteriaModel);

                parseFstabAndPersistDiskInformation(fstabInformation, stack);

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
