package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.DiskValidator;
import com.sequenceiq.cloudbreak.util.CodUtil;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

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
    private HostOrchestrator hostOrchestrator;

    @Inject
    private DiskValidator diskValidator;

    @Inject
    private EntitlementService entitlementService;

    public void mountAllDisks(Long stackId) throws CloudbreakException {
        LOGGER.debug("Mount all disks for stack.");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        updateResourceForStack(stackId, stack);
        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> allNodesWithDiskData = stackUtil.collectNodesWithDiskData(stack);
        mountDisks(stack, allNodesWithDiskData, allNodes);
    }

    public void mountDisksOnNewNodes(Long stackId, Set<String> upscaleCandidateAddresses, Set<Node> targetNodes) throws CloudbreakException {
        LOGGER.debug("Mount disks on new nodes: {}", upscaleCandidateAddresses);
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        updateResourceForStack(stackId, stack);
        Set<Node> newNodesWithDiskData = stackUtil.collectNewNodesWithDiskData(stack, upscaleCandidateAddresses);
        mountDisks(stack, newNodesWithDiskData, targetNodes);
    }

    private void updateResourceForStack(Long stackId, Stack stack) {
        ResourceType diskResourceType = stack.getDiskResourceType();
        if (diskResourceType != null) {
            stack.setResources(new HashSet<>(resourceService.findAllByStackIdAndResourceTypeIn(stackId, List.of(diskResourceType))));
            LOGGER.debug("Retrieved stack resources of type: {}, resources: {}", diskResourceType, stack.getResources());
        }
    }

    private void mountDisks(Stack stack, Set<Node> nodesWithDiskData, Set<Node> allNodes) throws CloudbreakException {
        Cluster cluster = stack.getCluster();
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), cluster.getId());

            boolean xfsForEphemeralDisksSupported =
                    entitlementService.isXfsForEphemeralDisksSupported(Crn.safeFromString(stack.getResourceCrn()).getAccountId()) &&
                            StackType.WORKLOAD.equals(stack.getType()) &&
                            CloudPlatform.AWS.equals(CloudPlatform.fromName(stack.getCloudPlatform())) &&
                            CodUtil.isCodCluster(stack) &&
                            isVersionNewerOrEqualThanLimited(stack.getStackVersion(), CLOUDERA_STACK_VERSION_7_3_1);
            hostOrchestrator.updateMountDiskPillar(stack, gatewayConfigs, stackUtil.collectNodesWithDiskData(stack), exitCriteriaModel,
                    stack.getPlatformVariant(), xfsForEphemeralDisksSupported);
            LOGGER.debug("Execute format and mount states.");
            Map<String, Map<String, String>> mountInfo =
                    hostOrchestrator.formatAndMountDisksOnNodes(stack, gatewayConfigs, nodesWithDiskData, allNodes, exitCriteriaModel);

            diskValidator.validateDisks(stack, nodesWithDiskData);

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
                        LOGGER.debug("Persisting fstab and uuids for instance: {}, uuids: {}, fstab: {}", instanceIdOptional.get(), uuids, fstab);
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
                        LOGGER.warn("DiscoveryFQDN is updated for {} from {} to {}",
                                volumeSet.getResourceName(), volumeSetAttributes.getDiscoveryFQDN(), discoveryFQDN);
                    }
                    volumeSetAttributes.setDiscoveryFQDN(discoveryFQDN);
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
    }
}
