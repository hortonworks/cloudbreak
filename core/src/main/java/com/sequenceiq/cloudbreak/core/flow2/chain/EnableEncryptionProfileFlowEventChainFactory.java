package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.EnableEncryptionProfileOnClusterStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigurationUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.EnableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.EnableEncryptionProfileTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class EnableEncryptionProfileFlowEventChainFactory implements FlowEventChainFactory<EnableEncryptionProfileTriggerEvent> {

    @Override
    public String initEvent() {
        return FlowChainTriggers.ENABLE_ENCRYPTION_PROFILE_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(EnableEncryptionProfileTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.add(new EnableEncryptionProfileOnClusterEvent(ENABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event(), event.getResourceId(),
                event.getEncryptionProfileCrn()));

        flowEventChain.add(new StackEvent(PillarConfigurationUpdateEvent.PILLAR_CONFIG_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));

        flowEventChain.add(new StackEvent(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), event.getResourceId(),
                event.accepted()));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
