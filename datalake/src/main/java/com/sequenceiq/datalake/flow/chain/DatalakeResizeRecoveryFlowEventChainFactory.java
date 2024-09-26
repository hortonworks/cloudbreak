package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent.SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_PEM_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachRecoveryEvent;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class DatalakeResizeRecoveryFlowEventChainFactory implements FlowEventChainFactory<DatalakeResizeRecoveryFlowChainStartEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeResizeRecoveryFlowEventChainFactory.class);

    @Override
    public String initEvent() {
        return SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeResizeRecoveryFlowChainStartEvent event) {
        Queue<Selectable> flowChain = new ConcurrentLinkedQueue<>();
        flowChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));

        if (event.getNewCluster() != null && event.getNewCluster().getDeleted() == null) {
            logEventCreation(SDX_DELETE_EVENT.event(), event.getNewCluster().getId());
            flowChain.add(createDetachEventForNewCluster(event));
            flowChain.add(new SdxDeleteStartEvent(
                    SDX_DELETE_EVENT.event(),
                    event.getNewCluster().getId(),
                    event.getUserId(),
                    true));
        }
        if (event.getOldCluster().isDetached()) {
            logEventCreation(SDX_DETACH_RECOVERY_EVENT.event(), event.getOldCluster().getId());
            flowChain.add(new SdxStartDetachRecoveryEvent(
                    SDX_DETACH_RECOVERY_EVENT.event(),
                    event.getOldCluster().getId(),
                    event.getUserId()));
        }
        logEventCreation(SDX_START_EVENT.event(), event.getOldCluster().getId());
        flowChain.add(new SdxStartStartEvent(
                SDX_START_EVENT.event(),
                event.getOldCluster().getId(),
                event.getUserId()));
        logEventCreation(UPDATE_LOAD_BALANCER_DNS_PEM_EVENT.event(), event.getOldCluster().getId());
        flowChain.add(new StartUpdateLoadBalancerDNSEvent(
                UPDATE_LOAD_BALANCER_DNS_PEM_EVENT.event(),
                event.getOldCluster().getId(),
                event.getOldCluster().getClusterName(),
                event.getUserId()));
        flowChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowChain);
    }

    private SdxStartDetachEvent createDetachEventForNewCluster(DatalakeResizeRecoveryFlowChainStartEvent event) {
        SdxCluster fauxCluster = new SdxCluster();
        fauxCluster.setClusterName(event.getNewCluster().getClusterName());

        logEventCreation(SDX_DETACH_EVENT.event(), event.getNewCluster().getId());
        SdxStartDetachEvent startDetachEvent = new SdxStartDetachEvent(
                SDX_DETACH_EVENT.event(),
                event.getNewCluster().getId(),
                fauxCluster,
                event.getUserId());
        startDetachEvent.setDetachDuringRecovery(true);
        return startDetachEvent;
    }

    private void logEventCreation(String event, Long datalakeId) {
        LOGGER.info("Generated a {} for datalake {} in the datalake resize recovery flow chain.",
                event, datalakeId);
    }
}
