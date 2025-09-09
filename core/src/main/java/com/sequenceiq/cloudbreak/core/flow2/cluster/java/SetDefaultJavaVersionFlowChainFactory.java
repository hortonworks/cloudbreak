package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event.ClusterServicesRestartTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class SetDefaultJavaVersionFlowChainFactory implements FlowEventChainFactory<SetDefaultJavaVersionTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(SetDefaultJavaVersionTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), event.getResourceId(),
                event.getDefaultJavaVersion(), event.isRestartServices(), event.isRestartCM(), event.isRollingRestart(), event.accepted()));
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        if (event.isRestartCM()) {
            flowEventChain.add(new StackEvent(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), event.getResourceId(),
                    event.accepted()));
        }
        if (event.isRestartServices()) {
            flowEventChain.add(new ClusterServicesRestartTriggerEvent(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), event.getResourceId(), true,
                    event.isRollingRestart(), false, true, event.accepted()));
        }
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

}
