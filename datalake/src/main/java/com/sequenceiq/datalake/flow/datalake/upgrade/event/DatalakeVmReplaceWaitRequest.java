package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeVmReplaceWaitRequest extends SdxEvent {

    public DatalakeVmReplaceWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static DatalakeVmReplaceWaitRequest from(SdxContext context) {
        return new DatalakeVmReplaceWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "DatalakeVmReplaceWaitRequest";
    }
}
