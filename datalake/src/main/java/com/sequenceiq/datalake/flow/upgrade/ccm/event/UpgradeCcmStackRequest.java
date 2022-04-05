package com.sequenceiq.datalake.flow.upgrade.ccm.event;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class UpgradeCcmStackRequest extends SdxEvent {

    public UpgradeCcmStackRequest(Long sdxId, String userId) {
        super(sdxId, userId);
    }

    public static UpgradeCcmStackRequest from(SdxContext context) {
        return new UpgradeCcmStackRequest(context.getSdxId(), context.getUserId());
    }

    @Override
    public String selector() {
        return UpgradeCcmStackRequest.class.getSimpleName();
    }
}

