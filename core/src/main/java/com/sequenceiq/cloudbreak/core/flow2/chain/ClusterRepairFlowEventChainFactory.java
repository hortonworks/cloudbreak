package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.ArrayList;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.repair.master.ha.ChangePrimaryGatewayEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ChangePrimaryGatewayTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.EphemeralClustersUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairFlowEventChainFactory.class);

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterService clusterService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        Stack stack = event.getStack();
        Queue<Selectable> flowChainTriggers = new ConcurrentLinkedDeque<>();
        Map<String, List<String>> failedNodesMap = event.getFailedNodesMap();
        boolean multipleGatewayStack = clusterService.isMultipleGateway(stack);
        for (Entry<String, List<String>> failedNodes : failedNodesMap.entrySet()) {
            String hostGroupName = failedNodes.getKey();
            List<String> hostNames = failedNodes.getValue();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
            boolean gatewayInstanceGroup = InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType());
            List<String> primaryGatewayHostNames = instanceMetaDataService.getPrimaryGatewayByInstanceGroup(stack.getId(), instanceGroup.getId()).stream()
                    .filter(imd -> hostNames.contains(imd.getDiscoveryFQDN()) && imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY)
                    .map(InstanceMetaData::getDiscoveryFQDN)
                    .collect(Collectors.toList());
            boolean primaryGatewayInstance = !primaryGatewayHostNames.isEmpty();
            boolean singlePrimaryGateway = primaryGatewayInstance && !multipleGatewayStack;

            List<String> failedHostnames = new ArrayList<>(hostNames);
            if (singlePrimaryGateway) {
                handleSinglePrimaryGateway(event, hostGroup, primaryGatewayHostNames, stack, flowChainTriggers);
                failedHostnames.removeAll(primaryGatewayHostNames);
                handleNotSinglePrimaryGateways(false, gatewayInstanceGroup, event, hostGroup, failedHostnames, stack, flowChainTriggers);
            } else {
                handleNotSinglePrimaryGateways(
                        primaryGatewayInstance, gatewayInstanceGroup, event, hostGroup, failedHostnames, stack, flowChainTriggers);
            }
        }
        return flowChainTriggers;
    }

    private void handleSinglePrimaryGateway(ClusterRepairTriggerEvent event, HostGroup hostGroup, List<String> hostNames, Stack stack,
            Queue<Selectable> flowChainTriggers) {
        InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
        addStackDownscale(event, flowChainTriggers, instanceGroup, stack, new HashSet<>(hostNames));
        if (!event.isRemoveOnly()) {
            addFullUpscale(event, flowChainTriggers, hostGroup.getName(), hostNames, true, isKerberosSecured(stack),
                    clusterService.isSingleNode(stack));
            // we need to update all ephemeral clusters that are connected to a datalake
            if (!stackService.findClustersConnectedToDatalakeByDatalakeStackId(event.getStackId()).isEmpty()) {
                upgradeEphemeralClusters(event, flowChainTriggers);
            }
        }
    }

    private void handleNotSinglePrimaryGateways(boolean primaryGatewayInstance, boolean gatewayInstanceGroup, ClusterRepairTriggerEvent event,
            HostGroup hostGroup, List<String> failedHostNames, Stack stack, Queue<Selectable> flowChainTriggers) {
        if (failedHostNames.isEmpty()) {
            return;
        }
        // TODO: handle the case when the gateway and the gateway candidates are all selected for repair
        if (gatewayInstanceGroup && primaryGatewayInstance) {
            addChangePrimaryGateway(event, flowChainTriggers);
        }
        addFullDownscale(event, stack, flowChainTriggers, hostGroup.getName(), failedHostNames);
        if (!event.isRemoveOnly()) {
            addFullUpscale(event, flowChainTriggers, hostGroup.getName(), failedHostNames, false, false,
                    clusterService.isSingleNode(stack));
            // we need to update all ephemeral clusters that are connected to a datalake
            if (gatewayInstanceGroup && !stackService.findClustersConnectedToDatalakeByDatalakeStackId(event.getStackId()).isEmpty() && primaryGatewayInstance) {
                upgradeEphemeralClusters(event, flowChainTriggers);
            }
        }
    }

    private void addStackDownscale(ClusterRepairTriggerEvent event, Queue<Selectable> flowChainTriggers, InstanceGroup instanceGroup, Stack stack,
            Set<String> hostNames) {
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
        flowChainTriggers.add(new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getStackId(), instanceGroup.getGroupName(),
                privateIdsForHostNames, event.accepted()));
    }

    private void addFullDownscale(ClusterRepairTriggerEvent event, Stack stack, Queue<Selectable> flowChainTriggers, String hostGroupName,
            List<String> hostNames) {
        Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), hostNames);
        flowChainTriggers.add(new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getStackId(),
                hostGroupName, Sets.newHashSet(privateIdsForHostNames), ScalingType.DOWNSCALE_TOGETHER, event.accepted(), new ClusterDownscaleDetails()));
    }

    private void addChangePrimaryGateway(ClusterRepairTriggerEvent event, Queue<Selectable> flowChainTriggers) {
        flowChainTriggers.add(new ChangePrimaryGatewayTriggerEvent(ChangePrimaryGatewayEvent.CHANGE_PRIMARY_GATEWAY_TRIGGER_EVENT.event(),
                event.getStackId(), event.accepted()));
    }

    private void upgradeEphemeralClusters(ClusterRepairTriggerEvent event, Queue<Selectable> flowChainTriggers) {
        flowChainTriggers.add(new EphemeralClustersUpgradeTriggerEvent(FlowChainTriggers.EPHEMERAL_CLUSTERS_UPDATE_TRIGGER_EVENT,
                event.getStackId(), event.accepted()));
    }

    private void addFullUpscale(ClusterRepairTriggerEvent event, Queue<Selectable> flowChainTriggers, String hostGroupName, List<String> hostNames,
            boolean singlePrimaryGateway, boolean kerberosSecured, boolean singleNodeCluster) {
        flowChainTriggers.add(new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getStackId(), hostGroupName,
                hostNames.size(), ScalingType.UPSCALE_TOGETHER, Sets.newHashSet(hostNames), singlePrimaryGateway,
                kerberosSecured, event.accepted(), singleNodeCluster));
    }

    private boolean isKerberosSecured(Stack stack) {
        return stack.getCluster().getKerberosConfig() != null;
    }
}
