package com.sequenceiq.datalake.flow.repair.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxRepairFailedEvent extends SdxFailedEvent {

    public SdxRepairFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static SdxRepairFailedEvent from(SdxEvent event, Exception exception) {
        return new SdxRepairFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "SdxRepairFailedEvent";
    }
}
