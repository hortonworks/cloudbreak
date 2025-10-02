package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.util.EphemeralVolumeUtil.volumeIsEphemeralWhichMustBeProvisioned;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.NodeVolumes;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class StackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUtil.class);

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Value("${cb.periscope.skipEntitlementCheckPlatforms}")
    private Set<String> skipStartStopEntitlementCheckPlatforms;

    public Set<Node> collectNodes(StackDtoDelegate stackDto) {
        return collectNodes(stackDto, emptySet());
    }

    public Set<Node> collectNodes(StackDtoDelegate stack, Set<String> hostNames) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroupDto instanceGroupDto : stack.getInstanceGroupDtos()) {
            if (!instanceGroupDto.getNotDeletedInstanceMetaData().isEmpty()) {
                InstanceGroupView instanceGroupView = instanceGroupDto.getInstanceGroup();
                for (InstanceMetadataView im : instanceGroupDto.getNotDeletedInstanceMetaData()) {
                    if (im.getDiscoveryFQDN() != null && (hostNames.isEmpty() || hostNames.contains(im.getDiscoveryFQDN()))) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroupView.getTemplate().getInstanceType();
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), instanceGroupView.getGroupName()));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectGatewayNodes(StackDto stack) {
        Set<Node> agents = stack.getAllPrimaryGatewayInstances().stream()
                .filter(imd -> imd.getDiscoveryFQDN() != null)
                .map(imd -> {
                    InstanceGroupDto instanceGroupDto = stack.getInstanceGroupByInstanceGroupName(imd.getInstanceGroupName());
                    return new Node(imd.getPrivateIp(), imd.getPublicIp(), imd.getInstanceId(),
                            instanceGroupDto.getInstanceGroup().getTemplate().getInstanceType(), imd.getDiscoveryFQDN(), imd.getInstanceGroupName());
                })
                .collect(Collectors.toSet());
        return agents;
    }

    public Set<Node> collectReachableNodesByInstanceStates(StackDto stackDto) {
        Set<Node> agents = new HashSet<>();
        stackDto.getReachableInstanceMetaDataSetWithInstanceGroup()
                .forEach(ig -> ig.getReachableInstanceMetaData().forEach(im -> {
                    String instanceId = im.getInstanceId();
                    String instanceType = ig.getInstanceGroup().getTemplate().getInstanceType();
                    agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType, im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                }));
        return agents;
    }

    public NodeReachabilityResult collectReachableAndUnreachableCandidateNodes(StackDto stack, Set<String> necessaryNodes) {
        NodeReachabilityResult nodeReachabilityResult = collectReachableAndUnreachableNodes(stack, necessaryNodes);
        Set<Node> reachableCandidateNodes = nodeReachabilityResult.getReachableNodes().stream()
                .filter(node -> necessaryNodes.contains(node.getHostname()))
                .collect(Collectors.toSet());
        Set<Node> unreachableCandidateNodes = nodeReachabilityResult.getUnreachableNodes().stream()
                .filter(node -> necessaryNodes.contains(node.getHostname()))
                .collect(Collectors.toSet());

        NodeReachabilityResult nodeReachabilityResultWithCandidates = new NodeReachabilityResult(reachableCandidateNodes, unreachableCandidateNodes);
        if (!unreachableCandidateNodes.isEmpty()) {
            LOGGER.warn("Some candidate nodes are unreachable: {}", nodeReachabilityResultWithCandidates.getUnreachableHosts());
        }
        LOGGER.debug("Candidate node reachability result: {}", nodeReachabilityResultWithCandidates);
        return nodeReachabilityResultWithCandidates;
    }

    public Set<Node> collectReachableAndCheckNecessaryNodes(StackDto stack, Collection<String> necessaryNodes) throws NodesUnreachableException {
        Set<Node> reachableNodes = collectReachableNodes(stack);
        Set<String> reachableAddresses = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        Set<String> unReachableCandidateNodes = necessaryNodes.stream()
                .filter(necessaryNodeAddress -> !reachableAddresses.contains(necessaryNodeAddress)).collect(Collectors.toSet());
        if (unReachableCandidateNodes.isEmpty()) {
            return reachableNodes;
        } else {
            LOGGER.error("Some of necessary nodes are unreachable: {}", unReachableCandidateNodes);
            throw new NodesUnreachableException("Some of necessary nodes are unreachable", unReachableCandidateNodes);
        }
    }

    public Set<Node> collectReachableNodes(StackDto stackDto) {
        return hostOrchestrator.getResponsiveNodes(collectNodes(stackDto), gatewayConfigService.getPrimaryGatewayConfig(stackDto), false).getReachableNodes();
    }

    public NodeReachabilityResult collectReachableAndUnreachableNodes(StackDto stackDto, Set<String> targets) {
        NodeReachabilityResult nodeReachabilityResult = hostOrchestrator.getResponsiveNodes(collectNodes(stackDto, targets),
                gatewayConfigService.getPrimaryGatewayConfig(stackDto), true);
        LOGGER.debug("Node reachability result: {}", nodeReachabilityResult);
        return nodeReachabilityResult;
    }

    public Set<Node> collectNodesWithDiskData(Stack stack) {
        return collectNewNodesWithDiskData(stack, Set.of());
    }

    public Set<Node> collectNewNodesWithDiskData(Stack stack, Set<String> newNodeAddresses) {
        Set<Node> agents = new HashSet<>();
        List<Resource> volumeSets = stack.getDiskResources();
        Map<String, Map<String, Object>> instanceToVolumeInfoMap = createInstanceToVolumeInfoMap(volumeSets);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && (newNodeAddresses.isEmpty() || newNodeAddresses.contains(im.getPrivateIp()))) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        String dataVolumes = getOrDefault(instanceToVolumeInfoMap, instanceId, "dataVolumes", "");
                        String dataVolumesWithDataLoss = getOrDefault(instanceToVolumeInfoMap, instanceId, "dataVolumesWithDataLoss", "");
                        String serialIds = getOrDefault(instanceToVolumeInfoMap, instanceId, "serialIds", "");
                        String serialIdsWithDataLoss = getOrDefault(instanceToVolumeInfoMap, instanceId, "serialIdsWithDataLoss", "");
                        String fstab = getOrDefault(instanceToVolumeInfoMap, instanceId, "fstab", "");
                        String uuids = getOrDefault(instanceToVolumeInfoMap, instanceId, "uuids", "");
                        Integer databaseVolumeIndex = getOrDefault(instanceToVolumeInfoMap, instanceId, "dataBaseVolumeIndex", -1);
                        TemporaryStorage temporaryStorage =
                                Optional.ofNullable(instanceGroup.getTemplate().getTemporaryStorage()).orElse(TemporaryStorage.ATTACHED_VOLUMES);
                        NodeVolumes nodeVolumes = new NodeVolumes(
                                databaseVolumeIndex,
                                dataVolumes,
                                dataVolumesWithDataLoss,
                                serialIds,
                                serialIdsWithDataLoss,
                                fstab,
                                uuids);
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType, im.getDiscoveryFQDN(), im.getInstanceGroupName(),
                                nodeVolumes, temporaryStorage));
                    }
                }
            }
        }
        return agents;
    }

    @VisibleForTesting
    Map<String, Map<String, Object>> createInstanceToVolumeInfoMap(List<Resource> volumeSets) {
        return volumeSets.stream()
                .filter(volumeSet -> StringUtils.isNotEmpty(volumeSet.getInstanceId()))
                .map(volumeSet -> Map.entry(volumeSet.getInstanceId(),
                        resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class)))
                .map(this::getVolumeInfo)
                // Preserve original order by using LinkedHashMap
                .collect(Collectors.toMap(
                        entry -> (String) entry.getKey(),
                        entry -> (Map<String, Object>) entry.getValue(),
                        (existing, replacement) -> existing,
                        java.util.LinkedHashMap::new));
    }

    private Map.Entry getVolumeInfo(Map.Entry<String, Optional<VolumeSetAttributes>> entry) {
        List<Volume> allVolumes = entry.getValue()
                .map(VolumeSetAttributes::getVolumes)
                .orElse(List.of());

        // Preserve the original order by maintaining index-based iteration
        List<Volume> volumes = new java.util.ArrayList<>();
        List<Volume> volumesWithDataLoss = new java.util.ArrayList<>();

        // Iterate through volumes in original order and separate them
        for (Volume volume : allVolumes) {
            if (!volumeIsEphemeralWhichMustBeProvisioned(volume)) {
                volumes.add(volume);
            } else {
                volumesWithDataLoss.add(volume);
            }
        }

        List<String> dataVolumes = volumes.stream().map(Volume::getDevice).collect(Collectors.toList());
        List<String> dataVolumesWithDataLoss = volumesWithDataLoss.stream().map(Volume::getDevice).collect(Collectors.toList());
        List<String> serialIds = volumes.stream().map(Volume::getId).collect(Collectors.toList());
        List<String> serialIdsWithDataLoss = volumesWithDataLoss.stream().map(Volume::getId).collect(Collectors.toList());
        LOGGER.debug("Datavolumes are {}, dataVolumesWithDataLoss are {}, serialIds are {}, serialIdsWithDataLoss are {}",
                dataVolumes, dataVolumesWithDataLoss, serialIds, serialIdsWithDataLoss);
        int dataBaseVolumeIndex = IntStream.range(0, volumes.size())
                .filter(index -> volumes.get(index).getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE)
                .findFirst()
                .orElse(-1);
        return Map.<String, Map<String, Object>>entry(entry.getKey(), Map.of(
                "dataVolumes", String.join(" ", dataVolumes),
                "dataVolumesWithDataLoss", String.join(" ", dataVolumesWithDataLoss),
                "serialIds", String.join(" ", serialIds),
                "serialIdsWithDataLoss", String.join(" ", serialIdsWithDataLoss),
                "dataBaseVolumeIndex", dataBaseVolumeIndex,
                "fstab", entry.getValue().map(VolumeSetAttributes::getFstab).orElse(""),
                "uuids", entry.getValue().map(VolumeSetAttributes::getUuids).orElse("")));
    }

    private <T> T getOrDefault(Map<String, Map<String, Object>> instanceToVolumeInfoMap, String instanceId, String innerKey, T defaultValue) {
        return (T) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault(innerKey, defaultValue);
    }

    public String extractClusterManagerIp(StackDtoDelegate stack) {
        return extractClusterManagerIp(stack.getCluster(), stack.getPrimaryGatewayInstance());
    }

    public String extractClusterManagerIp(ClusterView cluster, InstanceMetadataView primaryGatewayInstance) {
        if (cluster != null && !isEmpty(cluster.getClusterManagerIp())) {
            return cluster.getClusterManagerIp();
        }
        return primaryGatewayInstance == null ? null : primaryGatewayInstance.getPublicIpWrapper();
    }

    public String extractClusterManagerAddress(StackDtoDelegate stack) {
        String fqdn = loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(stack.getId());
        ClusterView cluster = stack.getCluster();
        fqdn = isEmpty(fqdn) ? cluster.getFqdn() : fqdn;

        if (isNotEmpty(fqdn)) {
            return fqdn;
        }

        String clusterManagerIp = cluster.getClusterManagerIp();

        if (isNotEmpty(clusterManagerIp)) {
            return clusterManagerIp;
        }

        return extractClusterManagerIp(stack);
    }

    public long getUptimeForCluster(Cluster cluster, boolean addUpsinceToUptime) {
        Duration uptime = Duration.ZERO;
        if (StringUtils.isNotBlank(cluster.getUptime())) {
            uptime = Duration.parse(cluster.getUptime());
        }
        if (cluster.getUpSince() != null && addUpsinceToUptime) {
            long now = new Date().getTime();
            uptime = uptime.plusMillis(now - cluster.getUpSince());
        }
        return uptime.toMillis();
    }

    public long getUptimeForCluster(ClusterView cluster, boolean addUpsinceToUptime) {
        Duration uptime = Duration.ZERO;
        if (StringUtils.isNotBlank(cluster.getUptime())) {
            uptime = Duration.parse(cluster.getUptime());
        }
        if (cluster.getUpSince() != null && addUpsinceToUptime) {
            long now = new Date().getTime();
            uptime = uptime.plusMillis(now - cluster.getUpSince());
        }
        return uptime.toMillis();
    }

    public CloudCredential getCloudCredential(String environmentCrn) {
        Credential credential = credentialClientService.getByEnvironmentCrn(environmentCrn);
        return credentialConverter.convert(credential);
    }

    public boolean stopStartScalingEntitlementEnabled(StackView stack) {
        boolean entitled = false;
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        String cloudPlatform = stack.getCloudPlatform();
        if (CloudPlatform.AWS.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.awsStopStartScalingEnabled(accountId);
        } else if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.azureStopStartScalingEnabled(accountId);
        } else if (CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform)) {
            entitled = entitlementService.gcpStopStartScalingEnabled(accountId);
        } else {
            entitled = skipStartStopEntitlementCheckPlatforms.contains(cloudPlatform);
        }
        return entitled;
    }

    public boolean stopStartScalingFailureRecoveryEnabled(StackView stack) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        return entitlementService.stopStartScalingFailureRecoveryEnabled(accountId);
    }
}
