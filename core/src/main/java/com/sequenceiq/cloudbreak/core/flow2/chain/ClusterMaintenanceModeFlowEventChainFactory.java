package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

@Component
public class ClusterMaintenanceModeFlowEventChainFactory implements FlowEventChainFactory<MaintenanceModeValidationTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT;
    }

    @Override
    public Queue<Selectable> createFlowTriggerEventQueue(MaintenanceModeValidationTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackSyncTriggerEvent(StackSyncEvent.STACK_SYNC_EVENT.event(), event.getStackId(), true, event.accepted()));
        flowEventChain.add(new StackEvent(CLUSTER_SYNC_EVENT.event(), event.getStackId()));
        flowEventChain.add(new MaintenanceModeValidationTriggerEvent(
                MaintenanceModeValidationEvent.START_VALIDATION_FLOW_EVENT.event(), event.getStackId()));
        return flowEventChain;
    }
}
