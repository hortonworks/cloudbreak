package com.sequenceiq.datalake.flow.datalake.cmsync.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxCmSyncWaitEvent extends SdxEvent {
    public SdxCmSyncWaitEvent(SdxContext context) {
        super(context);
    }
}
