package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent.SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachRecoveryEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DatalakeResizeRecoveryFlowEventChainFactory implements FlowEventChainFactory<DatalakeResizeRecoveryFlowChainStartEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeResizeRecoveryFlowEventChainFactory.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Override
    public String initEvent() {
        return SDX_RESIZE_RECOVERY_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeResizeRecoveryFlowChainStartEvent event) {
        Queue<Selectable> flowChain = new ConcurrentLinkedQueue<>();
        if (event.getNewCluster() != null && event.getNewCluster().getDeleted() == null) {
            flowChain.add(createDetachEventForNewCluster(event));
            flowChain.add(createForcedDeleteStartEventForNewCluster(event));
        }
        if (event.getOldCluster().isDetached()) {
            flowChain.add(createStartDetachRecoveryEventForOldCluster(event));
        }
        flowChain.add(createStartEventForOldCluster(event));
        return new FlowTriggerEventQueue(getName(), event, flowChain);
    }

    private SdxStartDetachEvent createDetachEventForNewCluster(DatalakeResizeRecoveryFlowChainStartEvent event) {
        SdxCluster fauxCluster = new SdxCluster();
        fauxCluster.setClusterName(event.getNewCluster().getClusterName());

        LOGGER.info("Generated a " + SDX_DETACH_EVENT.event() + " for the datalake resize recovery flow chain.");
        SdxStartDetachEvent startDetachEvent = new SdxStartDetachEvent(
                SDX_DETACH_EVENT.event(), event.getNewCluster().getId(), fauxCluster, event.getUserId(), event.accepted()
        );
        startDetachEvent.setDetachDuringRecovery(true);
        return startDetachEvent;
    }

    private SdxDeleteStartEvent createForcedDeleteStartEventForNewCluster(DatalakeResizeRecoveryFlowChainStartEvent event) {
        LOGGER.info("Generated a " + SDX_DELETE_EVENT.event() + " for the datalake resize recovery flow chain.");
        return new SdxDeleteStartEvent(SDX_DELETE_EVENT.event(), event.getNewCluster().getId(), event.getUserId(), true);
    }

    private SdxStartDetachRecoveryEvent createStartDetachRecoveryEventForOldCluster(DatalakeResizeRecoveryFlowChainStartEvent event) {
        LOGGER.info("Generated a " + SDX_DETACH_RECOVERY_EVENT.event() + " for the datalake resize recovery flow chain.");
        return new SdxStartDetachRecoveryEvent(
                SDX_DETACH_RECOVERY_EVENT.event(), event.getOldCluster().getId(), event.getUserId(), event.accepted()
        );
    }

    private SdxStartStartEvent createStartEventForOldCluster(DatalakeResizeRecoveryFlowChainStartEvent event) {
        LOGGER.info("Generated a " + SDX_START_EVENT.event() + " for the datalake resize recovery flow chain.");
        return new SdxStartStartEvent(
                SDX_START_EVENT.event(), event.getOldCluster().getId(), event.getUserId(), event.accepted()
        );
    }
}
