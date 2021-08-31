package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.sync.ClusterSyncEvent.CLUSTER_SYNC_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class StackImageUpdateFlowEventChainFactory implements FlowEventChainFactory<StackImageUpdateTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(StackImageUpdateTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackSyncTriggerEvent(StackSyncEvent.STACK_SYNC_EVENT.event(), event.getResourceId(), true, event.accepted()));
        flowEventChain.add(new StackEvent(CLUSTER_SYNC_EVENT.event(), event.getResourceId()));
        flowEventChain.add(new StackImageUpdateTriggerEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_EVENT.event(), event.getResourceId(), event.getNewImageId(),
                event.getImageCatalogName(), event.getImageCatalogUrl()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
