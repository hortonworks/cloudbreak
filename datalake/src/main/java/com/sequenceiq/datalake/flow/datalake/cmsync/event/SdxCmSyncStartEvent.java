package com.sequenceiq.datalake.flow.datalake.cmsync.event;

import static com.sequenceiq.datalake.flow.datalake.cmsync.SdxCmSyncEvent.SDX_CM_SYNC_START_EVENT;

import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCmSyncStartEvent extends SdxEvent {
    public SdxCmSyncStartEvent(Long sdxId, String sdxName, String userId) {
        super(SDX_CM_SYNC_START_EVENT.event(), sdxId, sdxName, userId);
    }
}
