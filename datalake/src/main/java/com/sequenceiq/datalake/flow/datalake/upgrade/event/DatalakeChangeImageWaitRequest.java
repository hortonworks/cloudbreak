package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeChangeImageWaitRequest extends SdxEvent {

    private UpgradeOptionV4Response upgradeOption;

    public DatalakeChangeImageWaitRequest(Long sdxId, String userId, UpgradeOptionV4Response upgradeOption) {
        super(sdxId, userId);
        this.upgradeOption = upgradeOption;
    }

    public static DatalakeChangeImageWaitRequest from(SdxContext context, UpgradeOptionV4Response upgradeOption) {
        return new DatalakeChangeImageWaitRequest(context.getSdxId(), context.getUserId(), upgradeOption);
    }

    public UpgradeOptionV4Response getUpgradeOption() {
        return upgradeOption;
    }

    @Override
    public String selector() {
        return "DatalakeChangeImageWaitRequest";
    }
}
