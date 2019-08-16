package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairSuccessEvent extends SdxEvent {

    public SdxRepairSuccessEvent(Long sdxId, String userId, String requestId, String sdxCrn) {
        super(sdxId, userId, requestId, sdxCrn);
    }

    @Override
    public String selector() {
        return "SdxRepairSuccessEvent";
    }
}
