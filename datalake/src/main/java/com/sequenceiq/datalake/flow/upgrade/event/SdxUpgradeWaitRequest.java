package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeWaitRequest extends SdxEvent {

    public SdxUpgradeWaitRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static SdxUpgradeWaitRequest from(SdxContext context) {
        return new SdxUpgradeWaitRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return "SdxUpgradeWaitRequest";
    }
}
