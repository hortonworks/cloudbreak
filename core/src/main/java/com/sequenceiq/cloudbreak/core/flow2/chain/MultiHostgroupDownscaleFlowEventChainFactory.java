package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class MultiHostgroupDownscaleFlowEventChainFactory implements FlowEventChainFactory<MultiHostgroupClusterAndStackDownscaleTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(MultiHostgroupClusterAndStackDownscaleTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        for (Entry<String, Set<Long>> entry : event.getInstanceIdsByHostgroupMap().entrySet()) {
            ClusterScaleTriggerEvent cste;
            cste = new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), entry.getKey(), entry.getValue(), event.accepted(),
                    event.getDetails());
            flowEventChain.add(cste);
            if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
                StackScaleTriggerEvent sste;
                sste = new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), entry.getKey(), entry.getValue());
                flowEventChain.add(sste);
            }
        }
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
