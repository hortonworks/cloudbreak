package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.RestartInstancesEvent;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.start.config.ExternalDatabaseStartEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RestartInstancesWithRdsStartFlowChainFactory implements FlowEventChainFactory<RestartInstancesWithRdsStartEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.RESTART_INSTANCES_WITH_RDS_RESTART_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(RestartInstancesWithRdsStartEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        if (event.isRdsRestartRequired()) {
            flowEventChain.add(new StackEvent(ExternalDatabaseStartEvent.EXTERNAL_DATABASE_COMMENCE_START_EVENT.event(),
                    event.getResourceId(), event.accepted()));
        }
        flowEventChain.add(new RestartInstancesEvent(RestartEvent.RESTART_TRIGGER_EVENT.event(), event.getResourceId(), event.getInstanceIds()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
