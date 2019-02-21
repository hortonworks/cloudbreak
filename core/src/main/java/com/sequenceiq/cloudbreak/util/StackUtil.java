package com.sequenceiq.cloudbreak.util;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.gs.collections.impl.tuple.AbstractImmutableEntry;
import com.gs.collections.impl.tuple.ImmutableEntry;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class StackUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUtil.class);

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    public Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null) {
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectNodesWithDiskData(Stack stack) {
        Set<Node> agents = new HashSet<>();
        List<Resource> volumeSets = stack.getDiskResources();
        Map<String, Map<String, String>> instanceToVolumeInfoMap = createInstanceToVolumeInfoMap(volumeSets);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null) {
                        String dataVolumes = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("dataVolumes", "");
                        String serialIds = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("serialIds", "");
                        String fstab = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("fstab", "");
                        String uuids = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("uuids", "");
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getDiscoveryFQDN(), im.getInstanceGroupName(),
                                dataVolumes, serialIds, fstab, uuids));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectNewNodesWithDiskData(Stack stack, Set<String> newNodeAddresses) {
        Set<Node> agents = new HashSet<>();
        List<Resource> volumeSets = stack.getDiskResources();
        Map<String, Map<String, String>> instanceToVolumeInfoMap = createInstanceToVolumeInfoMap(volumeSets);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && newNodeAddresses.contains(im.getPrivateIp())) {
                        String dataVolumes = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("dataVolumes", "");
                        String serialIds = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("serialIds", "");
                        String fstab = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("fstab", "");
                        String uuids = instanceToVolumeInfoMap.getOrDefault(im.getInstanceId(), Map.of()).getOrDefault("uuids", "");
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getDiscoveryFQDN(), im.getInstanceGroupName(),
                                dataVolumes, serialIds, fstab, uuids));
                    }
                }
            }
        }
        return agents;
    }

    private Map<String, Map<String, String>> createInstanceToVolumeInfoMap(List<Resource> volumeSets) {
        return volumeSets.stream()
                .map(volumeSet -> new ImmutableEntry<>(volumeSet.getInstanceId(),
                        resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class)))
                .map(entry -> {
                    List<Volume> volumes = entry.getValue().map(VolumeSetAttributes::getVolumes).orElse(List.of());
                    List<String> dataVolumes = volumes.stream().map(Volume::getDevice).collect(Collectors.toList());
                    List<String> serialIds = volumes.stream().map(Volume::getId).collect(Collectors.toList());
                    return new ImmutableEntry<>(entry.getKey(), Map.of(
                            "dataVolumes", String.join(" ", dataVolumes),
                            "serialIds", String.join(" ", serialIds),
                            "fstab", entry.getValue().map(VolumeSetAttributes::getFstab).orElse(""),
                            "uuids", entry.getValue().map(VolumeSetAttributes::getUuids).orElse("")));
                })
                .collect(Collectors.toMap(AbstractImmutableEntry::getKey, AbstractImmutableEntry::getValue));
    }

    public String extractAmbariIp(StackView stackView) {
        return extractAmbariIp(stackView.getId(), stackView.getOrchestrator().getType(),
                stackView.getClusterView() != null ? stackView.getClusterView().getAmbariIp() : null);
    }

    public String extractAmbariIp(Stack stack) {
        return extractAmbariIp(stack.getId(), stack.getOrchestrator().getType(), stack.getCluster() != null ? stack.getCluster().getAmbariIp() : null);
    }

    private String extractAmbariIp(long stackId, String orchestratorName, String ambariIp) {
        String result = null;
        try {
            OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestratorName);
            if (orchestratorType != null && orchestratorType.containerOrchestrator()) {
                result = ambariIp;
            } else {
                InstanceMetaData gatewayInstance = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId);
                if (gatewayInstance != null) {
                    result = gatewayInstance.getPublicIpWrapper();
                }
            }
        } catch (CloudbreakException ex) {
            LOGGER.error("Could not resolve orchestrator type: ", ex);
        }
        return result;
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
}
