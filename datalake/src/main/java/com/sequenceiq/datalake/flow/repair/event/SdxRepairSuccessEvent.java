package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxRepairSuccessEvent extends SdxEvent {

    public SdxRepairSuccessEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    @Override
    public String selector() {
        return "SdxRepairSuccessEvent";
    }
}
