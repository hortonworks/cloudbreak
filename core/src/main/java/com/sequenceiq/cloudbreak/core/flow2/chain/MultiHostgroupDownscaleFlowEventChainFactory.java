package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.DECOMMISSION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.MultiHostgroupClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackScaleTriggerEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class MultiHostgroupDownscaleFlowEventChainFactory implements FlowEventChainFactory<MultiHostgroupClusterAndStackDownscaleTriggerEvent> {

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.FULL_DOWNSCALE_MULTIHOSTGROUP_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(MultiHostgroupClusterAndStackDownscaleTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        ClusterScaleTriggerEvent cste = new ClusterDownscaleTriggerEvent(DECOMMISSION_EVENT.event(), event.getResourceId(), Collections.emptyMap(),
                event.getPrivateIdsByHostgroupMap(), Collections.emptyMap(), event.accepted(), event.getDetails());
        flowEventChain.add(cste);
        if (event.getScalingType() == ScalingType.DOWNSCALE_TOGETHER) {
            CloudPlatformVariant cloudPlatformVariant = stackService.getPlatformVariantByStackId(event.getResourceId());
            StackScaleTriggerEvent sste = new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(),
                    Collections.emptyMap(), event.getPrivateIdsByHostgroupMap(), Collections.emptyMap(), cloudPlatformVariant.getVariant().value());
            flowEventChain.add(sste);
        }
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
