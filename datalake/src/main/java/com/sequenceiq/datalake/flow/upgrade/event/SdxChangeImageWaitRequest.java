package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOption;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxChangeImageWaitRequest extends SdxEvent {

    private UpgradeOption upgradeOption;

    public SdxChangeImageWaitRequest(Long sdxId, String userId, String requestId, UpgradeOption upgradeOption) {
        super(sdxId, userId, requestId);
        this.upgradeOption = upgradeOption;
    }

    public static SdxChangeImageWaitRequest from(SdxContext context, UpgradeOption upgradeOption) {
        return new SdxChangeImageWaitRequest(context.getSdxId(), context.getUserId(), context.getRequestId(), upgradeOption);
    }

    public UpgradeOption getUpgradeOption() {
        return upgradeOption;
    }

    @Override
    public String selector() {
        return "SdxChangeImageWaitRequest";
    }
}
