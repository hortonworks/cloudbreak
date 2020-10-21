package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeVmReplaceWaitRequest extends SdxEvent {

    public DatalakeVmReplaceWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public DatalakeVmReplaceWaitRequest(SdxContext context) {
        super(context);
    }
}
