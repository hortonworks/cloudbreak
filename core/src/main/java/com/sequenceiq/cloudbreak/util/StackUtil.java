package com.sequenceiq.cloudbreak.util;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.gs.collections.impl.tuple.AbstractImmutableEntry;
import com.gs.collections.impl.tuple.ImmutableEntry;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeVolumes;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class StackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUtil.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectReachableNodesByInstanceStates(Stack stack) {
        return stack.getInstanceGroups()
                .stream()
                .filter(ig -> ig.getNodeCount() != 0)
                .flatMap(ig -> ig.getReachableInstanceMetaDataSet().stream())
                .filter(im -> im.getDiscoveryFQDN() != null)
                .map(im -> {
                    String instanceId = im.getInstanceId();
                    String instanceType = im.getInstanceGroup().getTemplate().getInstanceType();
                    return new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                            im.getDiscoveryFQDN(), im.getInstanceGroupName());
                })
                .collect(Collectors.toSet());
    }

    public Set<Node> collectAndCheckReachableNodes(Stack stack, Collection<String> necessaryNodes) throws NodesUnreachableException {
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

    public Set<Node> collectReachableNodes(Stack stack) {
        return hostOrchestrator.getResponsiveNodes(collectNodes(stack), gatewayConfigService.getPrimaryGatewayConfig(stack));
    }

    public Set<Node> collectNodesFromHostnames(Stack stack, Set<String> hostnames) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getReachableInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && hostnames.contains(im.getDiscoveryFQDN())) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                    }
                }
            }
        }
        return agents;
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
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && (newNodeAddresses.isEmpty() || newNodeAddresses.contains(im.getPrivateIp()))) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        String dataVolumes = getOrDefault(instanceToVolumeInfoMap, instanceId, "dataVolumes", "");
                        String serialIds = getOrDefault(instanceToVolumeInfoMap, instanceId, "serialIds", "");
                        String fstab = getOrDefault(instanceToVolumeInfoMap, instanceId, "fstab", "");
                        String uuids = getOrDefault(instanceToVolumeInfoMap, instanceId, "uuids", "");
                        Integer databaseVolumeIndex = getOrDefault(instanceToVolumeInfoMap, instanceId, "dataBaseVolumeIndex", -1);
                        TemporaryStorage temporaryStorage =
                                Optional.ofNullable(instanceGroup.getTemplate().getTemporaryStorage()).orElse(TemporaryStorage.ATTACHED_VOLUMES);
                        NodeVolumes nodeVolumes = new NodeVolumes(databaseVolumeIndex, dataVolumes, serialIds, fstab, uuids);
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
                .map(volumeSet -> new ImmutableEntry<>(volumeSet.getInstanceId(),
                        resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class)))
                .map(entry -> {
                    List<Volume> volumes = entry.getValue().map(VolumeSetAttributes::getVolumes).orElse(List.of());
                    List<String> dataVolumes = volumes.stream().map(Volume::getDevice).collect(Collectors.toList());
                    List<String> serialIds = volumes.stream().map(Volume::getId).collect(Collectors.toList());
                    int dataBaseVolumeIndex = IntStream.range(0, volumes.size())
                            .filter(index -> volumes.get(index).getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE)
                            .findFirst()
                            .orElse(-1);
                    return new ImmutableEntry<String, Map<String, Object>>(entry.getKey(), Map.of(
                            "dataVolumes", String.join(" ", dataVolumes),
                            "serialIds", String.join(" ", serialIds),
                            "dataBaseVolumeIndex", dataBaseVolumeIndex,
                            "fstab", entry.getValue().map(VolumeSetAttributes::getFstab).orElse(""),
                            "uuids", entry.getValue().map(VolumeSetAttributes::getUuids).orElse("")));
                })
                .collect(Collectors.toMap(AbstractImmutableEntry::getKey, AbstractImmutableEntry::getValue));
    }

    private <T> T getOrDefault(Map<String, Map<String, Object>> instanceToVolumeInfoMap, String instanceId, String innerKey, T defaultValue) {
        return (T) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault(innerKey, defaultValue);
    }

    public String extractClusterManagerIp(StackView stackView) {
        return extractClusterManagerIp(stackView.getId());
    }

    public String extractClusterManagerIp(Stack stack) {
        if (!isEmpty(stack.getClusterManagerIp())) {
            return stack.getClusterManagerIp();
        }
        return extractClusterManagerIp(stack.getId());
    }

    private String extractClusterManagerIp(long stackId) {
        AtomicReference<String> result = new AtomicReference<>(null);
        instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId).ifPresent(imd -> result.set(imd.getPublicIpWrapper()));
        return result.get();
    }

    public String extractClusterManagerAddress(Stack stack) {
        String fqdn = loadBalancerConfigService.getLoadBalancerUserFacingFQDN(stack.getId());
        fqdn = isEmpty(fqdn) ? stack.getFqdn() : fqdn;

        if (isNotEmpty(fqdn)) {
            return fqdn;
        }

        String clusterManagerIp = stack.getClusterManagerIp();

        if (isNotEmpty(clusterManagerIp)) {
            return clusterManagerIp;
        }

        return extractClusterManagerIp(stack.getId());
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

    public CloudCredential getCloudCredential(Stack stack) {
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        return credentialConverter.convert(credential);
    }
}
