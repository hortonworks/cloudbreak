package com.sequenceiq.datalake.flow.refresh.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatahubRefreshWaitEvent extends SdxEvent {
    public DatahubRefreshWaitEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }

}
