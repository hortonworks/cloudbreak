package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate.ClusterCertificatesRotationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterCertificatesRotationTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class RotateClusterCertificatesFlowEventChainFactory implements FlowEventChainFactory<ClusterCertificatesRotationTriggerEvent> {
    @Override
    public String initEvent() {
        return FlowChainTriggers.ROTATE_CLUSTER_CERTIFICATES_CHAIN_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterCertificatesRotationTriggerEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new StackEvent(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), event.getResourceId(), event.accepted()));
        flowEventChain.add(new ClusterCertificatesRotationTriggerEvent(ClusterCertificatesRotationEvent.CLUSTER_CMCA_ROTATION_EVENT.event(),
                event.getResourceId(), event.accepted(), event.getCertificateRotationType()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }
}
