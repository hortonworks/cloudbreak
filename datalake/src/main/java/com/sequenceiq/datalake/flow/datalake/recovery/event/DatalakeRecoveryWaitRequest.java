package com.sequenceiq.datalake.flow.datalake.recovery.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeRecoveryWaitRequest extends SdxEvent {

    public DatalakeRecoveryWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static DatalakeRecoveryWaitRequest from(SdxContext context) {
        return new DatalakeRecoveryWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "DatalakeRecoveryWaitRequest";
    }
}
