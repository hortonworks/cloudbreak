package com.sequenceiq.datalake.flow.refresh.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class DatahubRefreshFailedEvent extends SdxFailedEvent {

    public DatahubRefreshFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }
}
