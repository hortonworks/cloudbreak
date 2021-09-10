package com.sequenceiq.datalake.flow.detach.event;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeResizeFlowChainStartEvent extends SdxEvent {

    public static final String SDX_RESIZE_FLOW_CHAIN_START_EVENT = "DatalakeResizeFlowChainStartEvent";

    private SdxCluster sdxCluster;

    public DatalakeResizeFlowChainStartEvent(Long sdxId, SdxCluster newSdxCluster, String userId) {
        super(sdxId, userId);
        this.sdxCluster = newSdxCluster;
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }

    @Override
    public String selector() {
        return SDX_RESIZE_FLOW_CHAIN_START_EVENT;
    }
}
