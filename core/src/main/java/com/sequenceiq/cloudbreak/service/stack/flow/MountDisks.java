package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class MountDisks {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountDisks.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ResourceService resourceService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    public void mountAllDisks(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }

        Set<Node> nodes = stackUtil.collectNodesWithDiskData(stack);
        mountDisks(stackId, stack, nodes);
    }

    private void mountDisks(Long stackId, Stack stack, Set<Node> nodes) throws CloudbreakException {
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.containerOrchestrator()) {
            return;
        }

        Cluster cluster = stack.getCluster();
        try {
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(orchestrator.getType());
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());
            Map<String, Map<String, String>> mountInfo =
                    hostOrchestrator.formatAndMountDisksOnNodes(gatewayConfigs, nodes, exitCriteriaModel, stack.getPlatformVariant());

            mountInfo.forEach((hostname, value) -> {
                String instanceId = stack.getInstanceMetaDataAsList().stream()
                        .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                        .filter(instanceMetaData -> InstanceStatus.CREATED.equals(instanceMetaData.getInstanceStatus()))
                        .map(InstanceMetaData::getInstanceId)
                        .findFirst().get();

                String uuids = value.getOrDefault("uuids", "");
                String fstab = value.getOrDefault("fstab", "");
                if (!StringUtils.isEmpty(uuids) || !StringUtils.isEmpty(fstab)) {
                    persistUuidAndFstab(stack, instanceId, uuids, fstab);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to get orchestrator by type: {}", orchestrator.getType());
            throw new CloudbreakSecuritySetupException(e);
        }
    }

    private void persistUuidAndFstab(Stack stack, String instanceId, String uuids, String fstab) {
        resourceService.saveAll(stack.getDiskResources().stream()
                .filter(volumeSet -> instanceId.equals(volumeSet.getInstanceId()))
                .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                    volumeSetAttributes.setUuids(uuids);
                    volumeSetAttributes.setFstab(fstab);
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
    }

    public void mountDisksOnNewNodes(Long stackId, Set<String> upscaleCandidateAddresses) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }

        Set<Node> nodes = stackUtil.collectNewNodesWithDiskData(stack, upscaleCandidateAddresses);
        mountDisks(stackId, stack, nodes);
    }
}