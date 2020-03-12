package com.sequenceiq.cloudbreak.core.flow2.chain;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_EVENT;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<DatalakeClusterUpgradeTriggerEvent> {

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(DatalakeClusterUpgradeTriggerEvent event) {

        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new DatalakeClusterUpgradeTriggerEvent(CLUSTER_MANAGER_UPGRADE_EVENT.event(), event.getResourceId(), event.getTargetImage()));
        Stack stack = stackService.getByIdWithListsInTransaction(event.getResourceId());
        Map<String, List<String>> nodes = getAllNodes(stack);
        flowEventChain.add(new ClusterRepairTriggerEvent(stack.getId(), nodes, Boolean.FALSE));

        return flowEventChain;
    }

    private Map<String, List<String>> getAllNodes(Stack stack) {
        return stack.getInstanceGroups().stream()
                    .map(instanceGroup -> Map.entry(instanceGroup.getGroupName(), new ArrayList<>(instanceGroup
                            .getNotTerminatedInstanceMetaDataSet()
                            .stream()
                            .map(InstanceMetaData::getDiscoveryFQDN)
                            .collect(Collectors.toList()))))
                    .filter(entry -> !entry.getValue().isEmpty())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
