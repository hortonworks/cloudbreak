package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.ChangePrimaryGatewayEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataRepository instanceMetadataRepository;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        Stack stack = stackService.getById(event.getStackId());
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        Map<String, List<String>> failedNodesMap = event.getFailedNodesMap();
        for (Map.Entry<String, List<String>> failedNodes : failedNodesMap.entrySet()) {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), failedNodes.getKey());
            InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
            if (instanceGroup.getInstanceGroupType() == InstanceGroupType.GATEWAY) {
                if (instanceGroup.getNodeCount() <= 1) {
                    LOGGER.warn("Gateway instancegroup cannot be repaired if its nodecount is less or equal to 1.");
                    continue;
                } else {
                    List<InstanceMetaData> primary = instanceMetadataRepository.findAllByInstanceGroup(instanceGroup).stream().filter(
                            imd -> failedNodes.getValue().contains(imd.getDiscoveryFQDN())
                                    && imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY).collect(Collectors.toList());
                    if (!primary.isEmpty()) {
                        flowChainTriggers.add(new ChangePrimaryGatewayTriggerEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT.event(),
                                event.getStackId(), event.accepted()));
                    }
                }
            }
            flowChainTriggers.add(new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getStackId(),
                    failedNodes.getKey(), new HashSet<>(failedNodes.getValue()), ScalingType.DOWNSCALE_TOGETHER, event.accepted()));
            if (!event.isRemoveOnly()) {
                flowChainTriggers.add(new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getStackId(),
                        failedNodes.getKey(), failedNodes.getValue().size(), ScalingType.UPSCALE_TOGETHER));
            }
        }
        return flowChainTriggers;
    }
}
