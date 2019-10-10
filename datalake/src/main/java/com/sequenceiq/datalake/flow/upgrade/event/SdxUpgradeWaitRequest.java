package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeWaitRequest extends SdxEvent {

    public SdxUpgradeWaitRequest(Long sdxId, String userId, String requestId) {
        super(sdxId, userId, requestId);
    }

    public static SdxUpgradeWaitRequest from(SdxContext context) {
        return new SdxUpgradeWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId());
    }

    @Override
    public String selector() {
        return "SdxUpgradeWaitRequest";
    }
}
