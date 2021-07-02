package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeResizeFlowChainStartEvent extends SdxEvent {

    private SdxCluster sdxCluster;

    public DatalakeResizeFlowChainStartEvent(Long sdxId, SdxCluster newSdxCluster, String userId) {
        super(sdxId, userId);
        this.sdxCluster = newSdxCluster;
    }

    public SdxCluster getsdxCluster() {
        return sdxCluster;
    }

    @Override
    public String selector() {
        return "DatalakeResizeFlowChainStartEvent";
    }
}
