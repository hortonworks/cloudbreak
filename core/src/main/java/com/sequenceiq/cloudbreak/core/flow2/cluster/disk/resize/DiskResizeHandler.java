package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FAILURE_EVENT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class DiskResizeHandler extends EventSenderAwareHandler<DiskResizeHandlerRequest> {

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

    protected DiskResizeHandler(EventSender eventSender) {
        super(eventSender);
    }

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
    public void accept(Event<DiskResizeHandlerRequest> diskResizeHandlerRequestEvent) {
        LOGGER.debug("Starting resizeDisks on DiskUpdateService");
        DiskResizeHandlerRequest payload = diskResizeHandlerRequestEvent.getData();
        Long stackId = payload.getResourceId();
        String instanceGroup = payload.getInstanceGroup();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ResourceType diskResourceType = stack.getDiskResourceType();
        if (diskResourceType != null) {
            LOGGER.debug("Collecting resources based on stack id {} and resource type {} filtered by instance group {}.", stackId, diskResourceType,
                    instanceGroup);
            List<Resource> resourceList = resourceService.findAllByStackIdAndInstanceGroupAndResourceTypeIn(stackId, instanceGroup, List.of(diskResourceType))
                    .stream().filter(res -> null != res.getInstanceId() && res.getResourceType().toString().contains("VOLUMESET")).toList();
            stack.setResources(new HashSet<>(resourceList));
        }
        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Cluster cluster = stack.getCluster();
        try {
            InMemoryStateStore.putStack(stackId, PollGroup.POLLABLE);
            Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            LOGGER.debug("Calling host orchestrator for resizing and fetching fstab information for nodes - {}", allNodes);
            Map<String, Map<String, String>> fstabInformation = hostOrchestrator.resizeDisksOnNodes(gatewayConfigs, nodesWithDiskData, allNodes,
                    exitCriteriaModel);

            parseFstabAndPersistDiskInformation(fstabInformation, stack);

            InMemoryStateStore.deleteStack(stackId);
            eventSender().sendEvent(new DiskResizeFinishedEvent(stackId), diskResizeHandlerRequestEvent.getHeaders());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Failed to resize disks", e);
            eventSender().sendEvent(new DiskResizeFailedEvent(FAILURE_EVENT.selector(), stack.getId(), stack.getResourceName(), stack.getResourceCrn(), e),
                    diskResizeHandlerRequestEvent.getHeaders());
        }
    }
}
