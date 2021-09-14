package com.sequenceiq.datalake.flow.datalake.cmsync.event;

import com.sequenceiq.datalake.flow.SdxFailedEvent;

public class SdxCmSyncFailedEvent extends SdxFailedEvent {

    public SdxCmSyncFailedEvent(Long sdxId, String userId, Exception exception) {
        super(sdxId, userId, exception);
    }
}
