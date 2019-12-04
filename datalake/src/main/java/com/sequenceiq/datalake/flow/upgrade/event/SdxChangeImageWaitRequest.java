package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxChangeImageWaitRequest extends SdxEvent {

    private UpgradeOptionV4Response upgradeOption;

    public SdxChangeImageWaitRequest(Long sdxId, String userId, UpgradeOptionV4Response upgradeOption) {
        super(sdxId, userId);
        this.upgradeOption = upgradeOption;
    }

    public static SdxChangeImageWaitRequest from(SdxContext context, UpgradeOptionV4Response upgradeOption) {
        return new SdxChangeImageWaitRequest(context.getSdxId(), context.getUserId(), upgradeOption);
    }

    public UpgradeOptionV4Response getUpgradeOption() {
        return upgradeOption;
    }

    @Override
    public String selector() {
        return "SdxChangeImageWaitRequest";
    }
}
