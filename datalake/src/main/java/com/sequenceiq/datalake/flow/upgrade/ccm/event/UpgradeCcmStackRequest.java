package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackRequest extends SdxEvent {

    private final SdxCluster sdxCluster;

    public UpgradeCcmStackRequest(Long sdxId, String userId, SdxCluster sdxCluster) {
        super(sdxId, userId);
        this.sdxCluster = sdxCluster;
    }

    public static UpgradeCcmStackRequest from(SdxContext context, SdxCluster sdxCluster) {
        return new UpgradeCcmStackRequest(context.getSdxId(), context.getUserId(), sdxCluster);
    }

    @Override
    public String selector() {
        return UpgradeCcmStackRequest.class.getSimpleName();
    }

    public SdxCluster getSdxCluster() {
        return sdxCluster;
    }
}

