package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.STORAGE_VALIDATION_WAIT_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent.SDX_RESIZE_FLOW_CHAIN_START_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@Component
public class DatalakeResizeFlowEventChainFactory implements FlowEventChainFactory<DatalakeResizeFlowChainStartEvent> {
    @Override
    public String initEvent() {
        return SDX_RESIZE_FLOW_CHAIN_START_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(DatalakeResizeFlowChainStartEvent event) {
        Queue<Selectable> chain = new ConcurrentLinkedQueue<>();

        // Stop datalake
        chain.add(new SdxStartStopEvent(SDX_STOP_EVENT.event(), event.getResourceId(), event.getUserId(), event.accepted()));

        // De-attach sdx from environment
        chain.add(new SdxStartDetachEvent(SDX_DETACH_EVENT.event(), event.getResourceId(), event.getSdxCluster(), event.getUserId()));

        // Create new
        chain.add(new SdxEvent(STORAGE_VALIDATION_WAIT_EVENT.event(), event.getResourceId(), event.getSdxCluster().getClusterName(), event.getUserId()));

        // Delete  De-attached Sdx
        chain.add(new SdxDeleteStartEvent(SDX_DELETE_EVENT.event(), event.getResourceId(), event.getUserId(), true));

        return new FlowTriggerEventQueue(getName(), event, chain);
    }
}
