package com.sequenceiq.cloudbreak.core.flow2.chain;


import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.DatalakeClusterUpgradeTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;

@Component
public class UpgradeDatalakeFlowEventChainFactory implements FlowEventChainFactory<DatalakeClusterUpgradeTriggerEvent> {

    @Inject
    private StackStatusService stackStatusService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.DATALAKE_CLUSTER_UPGRADE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(DatalakeClusterUpgradeTriggerEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();

        Optional<StackStatus> stackStatusOpt = stackStatusService.findFirstByStackIdOrderByCreatedDesc(event.getResourceId());
        StackStatus unknownStackStatus = new StackStatus();
        unknownStackStatus.setDetailedStackStatus(DetailedStackStatus.UNKNOWN);
        DetailedStackStatus detailedStackStatus = stackStatusOpt.orElse(unknownStackStatus).getDetailedStackStatus();
        if (DetailedStackStatus.CLUSTER_UPGRADE_FAILED.equals(detailedStackStatus)) {
            chain.add(new DatalakeClusterUpgradeTriggerEvent(
                    CLUSTER_MANAGER_UPGRADE_FINISHED_EVENT.event(), event.getResourceId(), event.accepted(), event.getTargetImage()));
        } else {
            chain.add(new DatalakeClusterUpgradeTriggerEvent(
                    CLUSTER_MANAGER_UPGRADE_EVENT.event(), event.getResourceId(), event.accepted(), event.getTargetImage()));
        }

        return chain;
    }
}
