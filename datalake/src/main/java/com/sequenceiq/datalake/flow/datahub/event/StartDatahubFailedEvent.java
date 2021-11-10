package com.sequenceiq.datalake.flow.datahub.event;

import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class StartDatahubFailedEvent extends SdxFailedEvent {
    public StartDatahubFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }

    public static StartDatahubFailedEvent from(SdxEvent event, Exception exception) {
        return new StartDatahubFailedEvent(event.getResourceId(), event.getUserId(), exception);
    }

    @Override
    public String selector() {
        return "StartDatahubFailedEvent";
    }
}
