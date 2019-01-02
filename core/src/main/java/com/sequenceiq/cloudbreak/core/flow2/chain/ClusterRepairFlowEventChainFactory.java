package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.ChangePrimaryGatewayEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClustersUpgradeTriggerEvent;
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
        StackView stackView = stackService.getViewByIdWithoutAuth(event.getStackId());
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        Map<String, List<String>> failedNodesMap = event.getFailedNodesMap();
        for (Entry<String, List<String>> failedNodes : failedNodesMap.entrySet()) {
            String hostGroupName = failedNodes.getKey();
            List<String> hostNames = failedNodes.getValue();
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stackView.getClusterView().getId(), hostGroupName);
            InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                List<InstanceMetaData> primary = instanceMetadataRepository.findAllByInstanceGroup(instanceGroup).stream().filter(
                        imd -> hostNames.contains(imd.getDiscoveryFQDN())
                                && imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY).collect(Collectors.toList());
                if (!primary.isEmpty()) {
                    flowChainTriggers.add(new ChangePrimaryGatewayTriggerEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT.event(),
                            event.getStackId(), event.accepted()));
                }
            }
            Stack stack = stackService.getByIdWithListsInTransaction(event.getStackId());
            Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
            flowChainTriggers.add(new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getStackId(),
                    hostGroupName, Sets.newHashSet(privateIdsForHostNames), ScalingType.DOWNSCALE_TOGETHER, event.accepted(), new ClusterDownscaleDetails()));
            if (!event.isRemoveOnly()) {
                flowChainTriggers.add(new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getStackId(),
                        hostGroupName, hostNames.size(), ScalingType.UPSCALE_TOGETHER, Sets.newHashSet(hostNames)));
                // we need to update all ephemeral clusters that are connected to a datalake
                if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())
                        && !stackService.findClustersConnectedToDatalakeByDatalakeStackId(event.getStackId()).isEmpty()) {
                    flowChainTriggers.add(new EphemeralClustersUpgradeTriggerEvent(FlowChainTriggers.EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT,
                            event.getStackId(), event.accepted()));
                }
            }
        }
        return flowChainTriggers;
    }
}
