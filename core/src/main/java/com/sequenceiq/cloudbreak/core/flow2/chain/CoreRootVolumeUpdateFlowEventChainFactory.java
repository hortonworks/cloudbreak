package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.CoreProviderTemplateUpdateFlowEvent.CORE_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreProviderTemplateUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.rootvolumeupdate.event.CoreRootVolumeUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class CoreRootVolumeUpdateFlowEventChainFactory implements FlowEventChainFactory<CoreRootVolumeUpdateTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.CORE_ROOT_VOLUME_UPDATE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(CoreRootVolumeUpdateTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new CoreProviderTemplateUpdateEvent(
                CORE_PROVIDER_TEMPLATE_UPDATE_TRIGGER_EVENT.event(),
                event.getResourceId(),
                event.getVolumeType(),
                event.getSize(),
                event.getGroup(),
                event.getDiskType()
        ));
        flowEventChain.add(new ClusterRepairTriggerEvent(FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT, event.getResourceId(),
                ClusterRepairTriggerEvent.RepairType.ONE_BY_ONE, event.getUpdatedNodesMap(), true, null, false));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
