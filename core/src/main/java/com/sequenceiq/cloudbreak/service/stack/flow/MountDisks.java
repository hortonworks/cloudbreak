package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class MountDisks {

    private static final String MIN_VERSION = "2.16.0";

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
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public void mountAllDisks(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }

        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack);
        mountDisks(stack, nodesWithDiskData, allNodes);
    }

    public void mountDisksOnNewNodes(Long stackId, Set<String> upscaleCandidateAddresses, Set<Node> allNodes) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stackId)));
        if (!StackService.REATTACH_COMPATIBLE_PLATFORMS.contains(stack.getPlatformVariant())) {
            return;
        }
        Set<Node> nodesWithDiskData = stackUtil.collectNewNodesWithDiskData(stack, upscaleCandidateAddresses);
        mountDisks(stack, nodesWithDiskData, allNodes);
    }

    private void mountDisks(Stack stack, Set<Node> nodesWithDiskData, Set<Node> allNodes) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());

            Map<String, Map<String, String>> mountInfo;
            if (isCbVersionPostOptimisation(stack)) {
                mountInfo = hostOrchestrator.formatAndMountDisksOnNodes(gatewayConfigs, nodesWithDiskData, allNodes, exitCriteriaModel,
                        stack.getPlatformVariant());
            } else {
                mountInfo = hostOrchestrator.formatAndMountDisksOnNodesLegacy(gatewayConfigs, nodesWithDiskData, allNodes, exitCriteriaModel,
                        stack.getPlatformVariant());
            }

            mountInfo.forEach((hostname, value) -> {
                Optional<String> instanceIdOptional = stack.getInstanceMetaDataAsList().stream()
                        .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                        .filter(instanceMetaData -> InstanceStatus.CREATED.equals(instanceMetaData.getInstanceStatus()))
                        .map(InstanceMetaData::getInstanceId)
                        .findFirst();

                if (instanceIdOptional.isPresent()) {
                    String uuids = value.getOrDefault("uuids", "");
                    String fstab = value.getOrDefault("fstab", "");
                    if (!StringUtils.isEmpty(uuids) && !StringUtils.isEmpty(fstab)) {
                        persistUuidAndFstab(stack, instanceIdOptional.get(), hostname, uuids, fstab);
                    }
                }
            });
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Failed to mount disks", e);
            throw new CloudbreakSecuritySetupException(e);
        }
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

    private boolean isCbVersionPostOptimisation(Stack stack) {
        CloudbreakDetails cloudbreakDetails = componentConfigProviderService.getCloudbreakDetails(stack.getId());
        VersionComparator versionComparator = new VersionComparator();
        String version = substringBefore(cloudbreakDetails.getVersion(), "-");
        int compare = versionComparator.compare(() -> version, () -> MIN_VERSION);
        return compare >= 0;
    }
}